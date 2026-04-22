package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LLM-as-judge evaluators aligned with Phoenix-style metrics (faithfulness, relevance, correctness, conciseness).
 */
@Service
public class EvaluatorService {

  private final OpenAiChatModel openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final ObjectMapper objectMapper;
  private final AiLimitsProperties aiLimitsProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final AiBudgetService aiBudgetService;

  public EvaluatorService(
      OpenAiChatModel openAiChatModel,
      ObjectProvider<AnthropicChatModel> anthropicChatModel,
      ObjectMapper objectMapper,
      AiLimitsProperties aiLimitsProperties,
      AiCircuitBreaker aiCircuitBreaker,
      AiBudgetService aiBudgetService) {
    this.openAiChatModel = openAiChatModel;
    this.anthropicChatModel = anthropicChatModel;
    this.objectMapper = objectMapper;
    this.aiLimitsProperties = aiLimitsProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.aiBudgetService = aiBudgetService;
  }

  public EvaluationScore evaluateFaithfulness(
      String evaluatorModelId,
      String question,
      String response,
      List<String> documentTexts) {
    String ctx = documentTexts.stream()
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining("\n---\n"));
    String prompt = """
        You are an expert evaluator. Determine if the assistant response is faithful to the provided context.
        A faithful response only states claims that are supported by the context; hallucinations or unsupported claims lower the score.

        Question: %s

        Context documents:
        %s

        Assistant response:
        %s

        Return ONLY a single JSON object with double-quoted keys: {"score": <number 0.0-1.0>, "label": "<faithful|partial|unfaithful>", "explanation": "<brief>"}
        """.formatted(
        escapeTemplate(question),
        escapeTemplate(ctx.isBlank() ? "(no documents)" : ctx),
        escapeTemplate(response));
    return invokeJudge(evaluatorModelId, prompt);
  }

  public EvaluationScore evaluateRelevance(
      String evaluatorModelId,
      String question,
      String response) {
    String prompt = """
        You are an expert evaluator. Score how relevant the assistant response is to the user question.
        Irrelevant tangents, refusals without cause, or missing the point should lower the score.

        Question: %s

        Assistant response:
        %s

        Return ONLY a single JSON object: {"score": <number 0.0-1.0>, "label": "<high|medium|low>", "explanation": "<brief>"}
        """.formatted(escapeTemplate(question), escapeTemplate(response));
    return invokeJudge(evaluatorModelId, prompt);
  }

  public EvaluationScore evaluateCorrectness(
      String evaluatorModelId,
      String question,
      String response,
      String referenceAnswer) {
    String ref = referenceAnswer == null ? "" : referenceAnswer;
    String prompt = """
        You are an expert evaluator. Given a reference answer (gold or expected), score how correct the assistant response is.
        Consider factual alignment and whether the response adequately answers the question compared to the reference.

        Question: %s

        Reference answer:
        %s

        Assistant response:
        %s

        Return ONLY a single JSON object: {"score": <number 0.0-1.0>, "label": "<correct|partial|incorrect>", "explanation": "<brief>"}
        """.formatted(escapeTemplate(question), escapeTemplate(ref), escapeTemplate(response));
    return invokeJudge(evaluatorModelId, prompt);
  }

  public EvaluationScore evaluateConciseness(
      String evaluatorModelId,
      String question,
      String response) {
    String prompt = """
        You are an expert evaluator. Score how concise the assistant response is for the question.
        Penalize unnecessary filler, hedging, repetition, and meta-commentary.

        Question: %s

        Assistant response:
        %s

        Return ONLY a single JSON object: {"score": <number 0.0-1.0>, "label": "<concise|ok|verbose>", "explanation": "<brief>"}
        """.formatted(escapeTemplate(question), escapeTemplate(response));
    return invokeJudge(evaluatorModelId, prompt);
  }

  private EvaluationScore invokeJudge(String evaluatorModelId, String userContent) {
    Optional<SupportedChatModel> resolved = SupportedChatModel.fromModelId(evaluatorModelId);
    if (resolved.isEmpty()) {
      return EvaluationScore.failed("Unknown evaluator model: " + evaluatorModelId);
    }
    SupportedChatModel model = resolved.get();
    try {
      aiCircuitBreaker.assertClosed();
      String instructions = "You output only valid JSON. No markdown fences.\n\n" + userContent;
      int judgeMax = aiLimitsProperties.getJudgeMaxTokens();
      ChatResponse chatResponse = switch (model.provider()) {
        case OPENAI -> {
          OpenAiChatOptions opts = OpenAiChatOptions.builder()
              .model(model.modelId())
              .maxCompletionTokens(judgeMax)
              .build();
          yield openAiChatModel.call(new Prompt(instructions, opts));
        }
        case ANTHROPIC -> {
          AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
          if (anthropic == null) {
            throw new IllegalStateException("Anthropic is not configured.");
          }
          AnthropicChatOptions opts = AnthropicChatOptions.builder()
              .model(model.modelId())
              .maxTokens(judgeMax)
              .build();
          yield anthropic.call(new Prompt(instructions, opts));
        }
      };
      recordEvaluatorUsage(model.modelId(), chatResponse);
      String text = chatResponse.getResult().getOutput().getText();
      return parseScoreJson(text);
    } catch (AiCircuitOpenException e) {
      throw e;
    } catch (Exception e) {
      return EvaluationScore.failed(e.getMessage());
    }
  }

  private EvaluationScore parseScoreJson(String raw) {
    if (raw == null || raw.isBlank()) {
      return EvaluationScore.failed("empty model output");
    }
    String trimmed = raw.strip();
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start < 0 || end <= start) {
      return EvaluationScore.failed("no JSON object in output");
    }
    String json = trimmed.substring(start, end + 1);
    try {
      JsonNode n = objectMapper.readTree(json);
      double score = n.path("score").asDouble(Double.NaN);
      String label = n.path("label").asText("");
      String explanation = n.path("explanation").asText("");
      if (Double.isNaN(score)) {
        return EvaluationScore.failed("missing score in JSON");
      }
      score = Math.max(0.0, Math.min(1.0, score));
      return new EvaluationScore(score, label, explanation);
    } catch (Exception e) {
      return EvaluationScore.failed("JSON parse: " + e.getMessage());
    }
  }

  private void recordEvaluatorUsage(String modelId, ChatResponse chatResponse) {
    int prompt = 0;
    int completion = 0;
    if (chatResponse != null && chatResponse.getMetadata() != null) {
      Usage u = chatResponse.getMetadata().getUsage();
      if (u != null) {
        prompt = safeTokenCount(u.getPromptTokens());
        completion = safeTokenCount(u.getCompletionTokens());
      }
    }
    aiBudgetService.recordUsage(
        AiBudgetService.systemEvaluatorUserId(),
        modelId,
        prompt,
        completion,
        false);
  }

  private static int safeTokenCount(Number n) {
    if (n == null) {
      return 0;
    }
    long v = n.longValue();
    if (v < 0) {
      return 0;
    }
    if (v > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) v;
  }

  private static String escapeTemplate(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("%", "%%");
  }
}
