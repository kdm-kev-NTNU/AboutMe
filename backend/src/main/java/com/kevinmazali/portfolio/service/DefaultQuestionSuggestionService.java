package com.kevinmazali.portfolio.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.DefaultQuestionSuggestionRequest;
import com.kevinmazali.portfolio.model.DefaultQuestionSuggestionResponse;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.util.AiRequestContext;
import com.kevinmazali.portfolio.util.InputValidator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * One-shot LLM call over chunk text to propose default chatbot starter questions (admin-only).
 */
@Slf4j
@Service
public class DefaultQuestionSuggestionService {

  static final String SOURCE_CURRENT = "currentChunks";
  static final String SOURCE_UPLOADED = "uploadedJson";

  private static final int CHUNK_PAGE = 200;
  private static final int MAX_CHUNKS_TOTAL = 400;
  private static final int MAX_UPLOAD_JSON_CHARS = 2_000_000;
  private static final int MAX_CORPUS_CHARS = 80_000;
  private static final int MAX_CHUNK_TEXT_FOR_CORPUS = 4000;
  private static final String PROMPT_PATH = "prompts/default_chat_questions.txt";

  private final DocumentIngestionService documentIngestionService;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<OpenAiChatModel> openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final AiLimitsProperties aiLimitsProperties;
  private final AiBudgetProperties aiBudgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final AiBudgetService aiBudgetService;
  private final ChatModelCatalog chatModelCatalog;

  private volatile String promptTemplate;

  public DefaultQuestionSuggestionService(
      DocumentIngestionService documentIngestionService,
      ObjectMapper objectMapper,
      ObjectProvider<OpenAiChatModel> openAiChatModel,
      ObjectProvider<AnthropicChatModel> anthropicChatModel,
      AiLimitsProperties aiLimitsProperties,
      AiBudgetProperties aiBudgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      AiBudgetService aiBudgetService,
      ChatModelCatalog chatModelCatalog) {
    this.documentIngestionService = documentIngestionService;
    this.objectMapper = objectMapper;
    this.openAiChatModel = openAiChatModel;
    this.anthropicChatModel = anthropicChatModel;
    this.aiLimitsProperties = aiLimitsProperties;
    this.aiBudgetProperties = aiBudgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.aiBudgetService = aiBudgetService;
    this.chatModelCatalog = chatModelCatalog;
  }

  public DefaultQuestionSuggestionResponse suggest(DefaultQuestionSuggestionRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("body required");
    }
    String sourceRaw = req.source() == null ? "" : req.source().trim();
    String source = sourceRaw.equalsIgnoreCase(SOURCE_UPLOADED)
        ? SOURCE_UPLOADED
        : sourceRaw.equalsIgnoreCase(SOURCE_CURRENT) ? SOURCE_CURRENT : "";
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source must be \"" + SOURCE_CURRENT + "\" or \"" + SOURCE_UPLOADED + "\"");
    }

    List<ChunkItem> chunks;
    if (SOURCE_UPLOADED.equals(source)) {
      if (!StringUtils.hasText(req.chunksJson())) {
        throw new IllegalArgumentException("chunksJson is required when source is " + SOURCE_UPLOADED);
      }
      String raw = req.chunksJson().trim();
      if (raw.length() > MAX_UPLOAD_JSON_CHARS) {
        throw new IllegalArgumentException("chunksJson exceeds maximum allowed size");
      }
      chunks = parseChunksFromExportJson(raw);
    } else {
      String docFilter = StringUtils.hasText(req.documentId()) ? req.documentId().trim() : null;
      chunks = loadAllChunks(docFilter);
    }

    if (chunks.isEmpty()) {
      throw new IllegalArgumentException("No chunks available for suggestions");
    }
    if (chunks.size() > MAX_CHUNKS_TOTAL) {
      chunks = new ArrayList<>(chunks.subList(0, MAX_CHUNKS_TOTAL));
    }

    if (!StringUtils.hasText(req.model())) {
      throw new IllegalArgumentException("model is required");
    }
    SupportedChatModel model =
        SupportedChatModel.fromModelId(req.model().trim())
            .orElseThrow(() -> new IllegalArgumentException("Unknown model: " + req.model()));
    if (!chatModelCatalog.isModelConfigured(model)) {
      throw new IllegalArgumentException("Model is not configured (API key / chat enabled).");
    }

    int maxQ =
        req.maxQuestions() == null ? 12 : Math.min(30, Math.max(3, req.maxQuestions()));
    String language =
        StringUtils.hasText(req.language()) ? req.language().trim() : "Norwegian";

    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(
        AiRequestContext.budgetUserIdentifier(aiBudgetProperties),
        AiRequestContext.isAnonymousInteractiveUser());

    String corpus = buildCorpus(chunks);
    if (corpus.isBlank()) {
      throw new IllegalArgumentException("Chunk text is empty after preprocessing");
    }

    String instructions = buildFilledPrompt(language, maxQ, corpus);

    try {
      long startNs = System.nanoTime();
      ChatResponse response = invokeModel(model, instructions);
      double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
      recordUsage(model, response, latencySec, instructions);

      String out = response.getResult().getOutput().getText();
      List<String> suggestions = parseAndDedupeQuestions(out, maxQ);
      if (suggestions.isEmpty()) {
        throw new IllegalArgumentException("Model returned no usable questions; try another model or shorten the corpus.");
      }
      return new DefaultQuestionSuggestionResponse(suggestions, model.modelId());
    } catch (BudgetExceededException | AiCircuitOpenException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Question suggestion parse failed: {}", e.toString());
      throw new IllegalArgumentException("Could not parse model output: " + e.getMessage());
    }
  }

  private List<ChunkItem> loadAllChunks(@Nullable String documentIdFilter) {
    List<ChunkItem> all = new ArrayList<>();
    int offset = 0;
    while (all.size() < MAX_CHUNKS_TOTAL) {
      int lim = Math.min(CHUNK_PAGE, MAX_CHUNKS_TOTAL - all.size());
      ChunkListResponse page = documentIngestionService.getChunks(documentIdFilter, lim, offset);
      if (page.chunks().isEmpty()) {
        break;
      }
      all.addAll(page.chunks());
      if (page.chunks().size() < lim) {
        break;
      }
      offset += lim;
    }
    return all;
  }

  List<ChunkItem> parseChunksFromExportJson(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode chunksNode = root.get("chunks");
      if (chunksNode == null || !chunksNode.isArray()) {
        throw new IllegalArgumentException("JSON must contain a top-level \"chunks\" array");
      }
      List<ChunkItem> items = new ArrayList<>();
      for (JsonNode n : chunksNode) {
        if (n == null || !n.isObject()) {
          continue;
        }
        ChunkItem item = objectMapper.treeToValue(n, ChunkItem.class);
        if (item != null) {
          items.add(item);
        }
      }
      return items;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid chunks JSON: " + e.getMessage());
    }
  }

  private String buildCorpus(List<ChunkItem> chunks) {
    StringBuilder sb = new StringBuilder();
    for (ChunkItem c : chunks) {
      String title = c.documentTitle() != null ? c.documentTitle() : "";
      String text = c.text() == null ? "" : c.text();
      if (text.length() > MAX_CHUNK_TEXT_FOR_CORPUS) {
        text = text.substring(0, MAX_CHUNK_TEXT_FOR_CORPUS) + "\n…";
      }
      String block = "### chunk_id=" + c.id() + " title=" + title + "\n" + text + "\n\n";
      if (sb.length() + block.length() > MAX_CORPUS_CHARS) {
        break;
      }
      sb.append(block);
    }
    return sb.toString().strip();
  }

  private String buildFilledPrompt(String language, int maxQuestions, String corpus) {
    return getPromptTemplate()
        .replace("{language}", language)
        .replace("{maxQuestions}", Integer.toString(maxQuestions))
        .replace("{corpus}", corpus);
  }

  private String getPromptTemplate() {
    String cached = promptTemplate;
    if (cached != null) {
      return cached;
    }
    synchronized (this) {
      if (promptTemplate != null) {
        return promptTemplate;
      }
      try {
        ClassPathResource res = new ClassPathResource(PROMPT_PATH);
        promptTemplate = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
        return promptTemplate;
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + PROMPT_PATH, e);
      }
    }
  }

  private ChatResponse invokeModel(SupportedChatModel model, String instructions) {
    int maxTokens = Math.min(1024, Math.max(256, aiLimitsProperties.getChatMaxCompletionTokens() * 2));
    String augmented = "You output only valid JSON. No markdown fences.\n\n" + instructions;
    return switch (model.provider()) {
      case OPENAI -> {
        OpenAiChatModel openAi = openAiChatModel.getIfAvailable();
        if (openAi == null) {
          throw new IllegalStateException("OpenAI chat is not configured.");
        }
        OpenAiChatOptions opts =
            OpenAiChatOptions.builder().model(model.modelId()).maxCompletionTokens(maxTokens).build();
        yield openAi.call(new Prompt(augmented, opts));
      }
      case ANTHROPIC -> {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
          throw new IllegalStateException("Anthropic chat is not configured.");
        }
        AnthropicChatOptions opts =
            AnthropicChatOptions.builder().model(model.modelId()).maxTokens(maxTokens).build();
        yield anthropic.call(new Prompt(augmented, opts));
      }
    };
  }

  private void recordUsage(
      SupportedChatModel model, ChatResponse chatResponse, double latencySeconds, String promptText) {
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
    String userId = AiRequestContext.budgetUserIdentifier(aiBudgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    AiGenerationAnalytics analytics = AiGenerationAnalytics.empty().withTexts(promptText, out, null);
    aiBudgetService.recordUsage(
        userId,
        model.modelId(),
        prompt,
        completion,
        anonymous,
        latencySeconds,
        "default_question_suggestions",
        analytics);
  }

  private List<String> parseAndDedupeQuestions(String raw, int maxQuestions) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("empty model output");
    }
    try {
      String trimmed = raw.strip();
      int start = trimmed.indexOf('{');
      int end = trimmed.lastIndexOf('}');
      if (start < 0 || end <= start) {
        throw new IllegalArgumentException("no JSON object in model output");
      }
      String json = trimmed.substring(start, end + 1);
      JsonNode root = objectMapper.readTree(json);
      JsonNode arr = root.get("questions");
      if (arr == null || !arr.isArray()) {
        throw new IllegalArgumentException("expected JSON object with \"questions\" array");
      }
      int maxQChars = aiLimitsProperties.getMaxQuestionChars();
      LinkedHashSet<String> seen = new LinkedHashSet<>();
      List<String> out = new ArrayList<>();
      for (JsonNode n : arr) {
        if (n == null || !n.isValueNode()) {
          continue;
        }
        String s = InputValidator.sanitizeString(n.asText().trim());
        if (!StringUtils.hasText(s) || !InputValidator.isValidQuestion(s, maxQChars)) {
          continue;
        }
        String lower = s.toLowerCase();
        if (seen.add(lower) && out.size() < maxQuestions) {
          out.add(s);
        }
      }
      return out;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid questions JSON: " + e.getMessage());
    }
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
}
