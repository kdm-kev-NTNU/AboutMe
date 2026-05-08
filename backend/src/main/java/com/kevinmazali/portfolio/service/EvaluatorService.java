package com.kevinmazali.portfolio.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
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
 * LLM-as-judge evaluators for standard RAG metrics (faithfulness, relevance, correctness, conciseness).
 */
@Service
public class EvaluatorService {

  private final ObjectProvider<OpenAiChatModel> openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final ObjectMapper objectMapper;
  private final AiLimitsProperties aiLimitsProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final AiBudgetService aiBudgetService;

  public EvaluatorService(
      ObjectProvider<OpenAiChatModel> openAiChatModel,
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

  /**
   * Evaluates whether the response language matches the question language.
   * The chatbot must reply in Norwegian when asked in Norwegian, and in English when asked in English.
   * Mixed-language responses (e.g. English answer to a Norwegian question) receive a low score.
   */
  public EvaluationScore evaluateLanguageConsistency(
      String evaluatorModelId,
      String question,
      String response) {
    String prompt = """
        You are an expert language evaluator for a bilingual (Norwegian / English) chatbot.

        Your task: determine whether the assistant response is written in the SAME language as the user question.
        The chatbot is required to reply in Norwegian when the question is in Norwegian, and in English when the question is in English.

        Scoring rubric:
        - 1.0 "consistent": The response is entirely in the same language as the question.
        - 0.7 "mostly_consistent": The response is mostly in the correct language but contains a few words or short phrases in the other language (e.g. proper nouns, technical terms, or minor slips).
        - 0.3 "mixed": The response is roughly half in each language, or large portions are in the wrong language.
        - 0.0 "wrong_language": The response is entirely or almost entirely in the wrong language (e.g. an English answer to a Norwegian question).

        Important rules:
        - Proper nouns, brand names, programming terms, and technical jargon that have no standard translation are acceptable in either language and should NOT lower the score.
        - If the question language is ambiguous (e.g. a single word that exists in both languages), be lenient and score 1.0 if the response is coherent in one language.
        - Focus on the prose/natural-language portions of the response, not code snippets or URLs.

        Question: %s

        Assistant response:
        %s

        Return ONLY a single JSON object: {"score": <number 0.0-1.0>, "label": "<consistent|mostly_consistent|mixed|wrong_language>", "explanation": "<brief reasoning>"}
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
      long startNs = System.nanoTime();
      ChatResponse chatResponse = switch (model.provider()) {
        case OPENAI -> {
          OpenAiChatModel openAi = openAiChatModel.getIfAvailable();
          if (openAi == null) {
            throw new IllegalStateException("OpenAI chat is not configured.");
          }
          OpenAiChatOptions opts = OpenAiChatOptions.builder()
              .model(model.modelId())
              .maxCompletionTokens(judgeMax)
              .build();
          yield openAi.call(new Prompt(instructions, opts));
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
      double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
      recordEvaluatorUsage(model.modelId(), chatResponse, latencySec, instructions);
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

  private void recordEvaluatorUsage(
      String modelId, ChatResponse chatResponse, double latencySeconds, String judgeInput) {
    int prompt = 0;
    int completion = 0;
    if (chatResponse != null && chatResponse.getMetadata() != null) {
      Usage u = chatResponse.getMetadata().getUsage();
      if (u != null) {
        prompt = safeTokenCount(u.getPromptTokens());
        completion = safeTokenCount(u.getCompletionTokens());
      }
    }
    String out =
        chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null
            ? chatResponse.getResult().getOutput().getText()
            : "";
    AiGenerationAnalytics analytics =
        AiGenerationAnalytics.empty().withTexts(judgeInput, out, null);
    aiBudgetService.recordUsage(
        AiBudgetService.systemEvaluatorUserId(),
        modelId,
        prompt,
        completion,
        false,
        latencySeconds,
        "llm_judge",
        analytics);
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
