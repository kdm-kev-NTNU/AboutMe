package com.kevinmazali.portfolio.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus;
import com.kevinmazali.portfolio.model.experiment.GeneratedQaItem;
import com.kevinmazali.portfolio.repository.DatasetGenerationRepository;
import com.kevinmazali.portfolio.util.AiRequestContext;
import com.kevinmazali.portfolio.util.InputValidator;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Async QRA pipeline: chunks from pgvector → LLM JSON Q/A → postprocess → dedupe → backfill → {@link EvalDatasetService}.
 */
@Slf4j
@Component
public class DatasetGenerationAsyncRunner {

  private static final int CHUNK_PAGE = 200;
  private static final int MAX_TEXT_CHARS = 2000;
  private static final double DEDUP_THRESHOLD = 0.85;
  private static final int MAX_BACKFILL_ROUNDS = 3;
  private static final String PROMPT_PATH = "prompts/question_generation.txt";

  private final DatasetGenerationRepository datasetGenerationRepository;
  private final DocumentIngestionService documentIngestionService;
  private final EvalDatasetService evalDatasetService;
  private final ObjectProvider<OpenAiChatModel> openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final ObjectMapper objectMapper;
  private final AiLimitsProperties aiLimitsProperties;
  private final AiBudgetProperties aiBudgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final AiBudgetService aiBudgetService;

  private volatile String promptTemplate;

  public DatasetGenerationAsyncRunner(
      DatasetGenerationRepository datasetGenerationRepository,
      DocumentIngestionService documentIngestionService,
      EvalDatasetService evalDatasetService,
      ObjectProvider<OpenAiChatModel> openAiChatModel,
      ObjectProvider<AnthropicChatModel> anthropicChatModel,
      ObjectMapper objectMapper,
      AiLimitsProperties aiLimitsProperties,
      AiBudgetProperties aiBudgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      AiBudgetService aiBudgetService) {
    this.datasetGenerationRepository = datasetGenerationRepository;
    this.documentIngestionService = documentIngestionService;
    this.evalDatasetService = evalDatasetService;
    this.openAiChatModel = openAiChatModel;
    this.anthropicChatModel = anthropicChatModel;
    this.objectMapper = objectMapper;
    this.aiLimitsProperties = aiLimitsProperties;
    this.aiBudgetProperties = aiBudgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.aiBudgetService = aiBudgetService;
  }

  @Async("experimentTaskExecutor")
  public void executeGeneration(Long generationId) {
    try {
      runInternal(generationId);
    } catch (BudgetExceededException | AiCircuitOpenException e) {
      fail(generationId, e.getMessage());
    } catch (Exception e) {
      log.warn("Dataset generation failed: {}", e.toString(), e);
      fail(generationId, truncate(e.getMessage(), 2000));
    }
  }

  private void fail(Long generationId, String message) {
    datasetGenerationRepository
        .findById(generationId)
        .ifPresent(
            g -> {
              g.setStatus(DatasetGenerationStatus.FAILED);
              g.setErrorMessage(message);
              g.setCompletedAt(OffsetDateTime.now());
              datasetGenerationRepository.save(g);
            });
  }

  private void runInternal(Long generationId) {
    DatasetGeneration genRow =
        datasetGenerationRepository.findById(generationId).orElseThrow(() -> new IllegalStateException("Missing generation " + generationId));

    String docFilter =
        StringUtils.hasText(genRow.getDocumentIdFilter()) ? genRow.getDocumentIdFilter().trim() : null;
    List<ChunkItem> chunks = loadAllChunks(docFilter);
    if (chunks.isEmpty()) {
      fail(generationId, "No vector chunks found for the selected scope. Ingest documents first.");
      return;
    }

    SupportedChatModel model =
        SupportedChatModel.fromModelId(genRow.getModel()).orElseThrow(() -> new IllegalStateException("Unknown model"));

    int qpc = Math.max(1, genRow.getQuestionsPerChunk());
    int targetTotal = computeTargetTotal(genRow, chunks.size(), qpc);
    if (targetTotal <= 0) {
      fail(generationId, "Computed target question count is 0");
      return;
    }

    Random rng =
        genRow.getSeed() != null ? new Random(genRow.getSeed().longValue()) : new Random();
    List<ChunkItem> shuffled = new ArrayList<>(chunks);
    Collections.shuffle(shuffled, rng);

    Set<String> attemptedChunkKeys = new HashSet<>();
    List<GeneratedQaItem> raw = new ArrayList<>();

    // --- Main phase ---
    for (ChunkItem chunk : shuffled) {
      if (raw.size() >= targetTotal) {
        break;
      }
      ChunkMeta meta = metaOf(chunk);
      String chunkKey = meta.chunkKey();
      for (int t = 0; t < qpc && raw.size() < targetTotal; t++) {
        if (t == 0) {
          attemptedChunkKeys.add(chunkKey);
        }
        tryGenerateAndAdd(model, chunk, meta, raw);
        maybeFlushProgress(generationId, raw.size());
      }
    }

    List<GeneratedQaItem> deduped = QaPostprocessor.deduplicateByJaccard(raw, DEDUP_THRESHOLD);

    // --- Backfill (Piscada-style): try unused chunks until target or rounds exhausted ---
    for (int round = 0; round < MAX_BACKFILL_ROUNDS && deduped.size() < targetTotal; round++) {
      List<ChunkItem> unused = new ArrayList<>();
      for (ChunkItem c : chunks) {
        if (!attemptedChunkKeys.contains(metaOf(c).chunkKey())) {
          unused.add(c);
        }
      }
      if (unused.isEmpty()) {
        break;
      }
      Collections.shuffle(unused, rng);
      List<GeneratedQaItem> extra = new ArrayList<>();
      for (ChunkItem chunk : unused) {
        if (deduped.size() + extra.size() >= targetTotal) {
          break;
        }
        ChunkMeta meta = metaOf(chunk);
        attemptedChunkKeys.add(meta.chunkKey());
        tryGenerateAndAdd(model, chunk, meta, extra);
        maybeFlushProgress(generationId, deduped.size() + extra.size());
      }
      if (extra.isEmpty()) {
        break;
      }
      List<GeneratedQaItem> combined = new ArrayList<>(deduped);
      combined.addAll(extra);
      deduped = QaPostprocessor.deduplicateByJaccard(combined, DEDUP_THRESHOLD);
    }

    List<GeneratedQaItem> finalItems = sanitizeForPersistence(deduped);
    if (finalItems.isEmpty()) {
      fail(generationId, "No valid questions passed sanitization after generation.");
      return;
    }

    List<CreateEvalDatasetRequest.DatasetExampleInput> examples =
        finalItems.stream()
            .map(i -> new CreateEvalDatasetRequest.DatasetExampleInput(i.question(), i.answer()))
            .toList();

    DatasetGeneration row = datasetGenerationRepository.findById(generationId).orElseThrow();
    String desc = StringUtils.hasText(row.getDescription()) ? row.getDescription().trim() : "";
    var created =
        evalDatasetService.createDataset(
            new CreateEvalDatasetRequest(row.getName().trim(), desc, examples));

    row.setStatus(DatasetGenerationStatus.COMPLETED);
    row.setResultDatasetId(Long.parseLong(created.id()));
    row.setQuestionsGenerated(finalItems.size());
    row.setCompletedAt(OffsetDateTime.now());
    row.setErrorMessage(null);
    datasetGenerationRepository.save(row);
  }

  private List<GeneratedQaItem> sanitizeForPersistence(List<GeneratedQaItem> items) {
    List<GeneratedQaItem> out = new ArrayList<>();
    int maxQ = aiLimitsProperties.getMaxQuestionChars();
    for (GeneratedQaItem it : items) {
      String q = InputValidator.sanitizeString(it.question());
      String a = InputValidator.sanitizeString(it.answer());
      if (!StringUtils.hasText(q) || !StringUtils.hasText(a)) {
        continue;
      }
      if (!InputValidator.isValidQuestion(q, maxQ)) {
        continue;
      }
      out.add(new GeneratedQaItem(q, a, it.sourceDocument(), it.documentId(), it.chunkIndex()));
    }
    return out;
  }

  private void maybeFlushProgress(long generationId, int count) {
    datasetGenerationRepository
        .findById(generationId)
        .ifPresent(
            g -> {
              g.setQuestionsGenerated(count);
              datasetGenerationRepository.save(g);
            });
  }

  private int computeTargetTotal(DatasetGeneration gen, int chunkCount, int qpc) {
    int capByChunks = qpc * chunkCount;
    if (gen.getMaxQuestions() != null) {
      return Math.min(Math.max(0, gen.getMaxQuestions()), capByChunks);
    }
    return capByChunks;
  }

  private List<ChunkItem> loadAllChunks(String documentIdFilter) {
    List<ChunkItem> all = new ArrayList<>();
    int offset = 0;
    while (true) {
      ChunkListResponse page = documentIngestionService.getChunks(documentIdFilter, CHUNK_PAGE, offset);
      if (page.chunks().isEmpty()) {
        break;
      }
      all.addAll(page.chunks());
      if (page.chunks().size() < CHUNK_PAGE) {
        break;
      }
      offset += CHUNK_PAGE;
    }
    return all;
  }

  private void tryGenerateAndAdd(
      SupportedChatModel model, ChunkItem chunk, ChunkMeta meta, List<GeneratedQaItem> sink) {
    try {
      aiCircuitBreaker.assertClosed();
      String text = chunk.text() == null ? "" : chunk.text();
      if (text.length() > MAX_TEXT_CHARS) {
        text = text.substring(0, MAX_TEXT_CHARS);
      }
      String filled = getPromptTemplate().replace("{text}", text);
      String instructions =
          "You output only valid JSON. No markdown fences.\n\n" + filled;

      long startNs = System.nanoTime();
      ChatResponse response = invokeQaModel(model, instructions);
      double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
      recordUsage(model, response, latencySec, instructions);

      String out = response.getResult().getOutput().getText();
      String[] qa = parseNormalizedQaJson(out);
      String[] cleaned = QaPostprocessor.cleanQaItem(qa[0], qa[1]);
      if (QaPostprocessor.isLowQuality(cleaned[0], cleaned[1])) {
        return;
      }
      sink.add(
          new GeneratedQaItem(
              cleaned[0], cleaned[1], meta.filename(), meta.documentId(), meta.chunkIndex()));
    } catch (BudgetExceededException | AiCircuitOpenException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Chunk QA generation skipped: {}", e.getMessage());
    }
  }

  private ChatResponse invokeQaModel(SupportedChatModel model, String instructions) {
    int maxTokens = Math.min(512, Math.max(128, aiLimitsProperties.getChatMaxCompletionTokens()));
    return switch (model.provider()) {
      case OPENAI -> {
        OpenAiChatModel openAi = openAiChatModel.getIfAvailable();
        if (openAi == null) {
          throw new IllegalStateException("OpenAI chat is not configured.");
        }
        OpenAiChatOptions opts =
            OpenAiChatOptions.builder().model(model.modelId()).maxCompletionTokens(maxTokens).build();
        yield openAi.call(new Prompt(instructions, opts));
      }
      case ANTHROPIC -> {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
          throw new IllegalStateException("Anthropic chat is not configured.");
        }
        AnthropicChatOptions opts =
            AnthropicChatOptions.builder().model(model.modelId()).maxTokens(maxTokens).build();
        yield anthropic.call(new Prompt(instructions, opts));
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
        userId, model.modelId(), prompt, completion, anonymous, latencySeconds, "qra_dataset_generation", analytics);
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

  private ChunkMeta metaOf(ChunkItem chunk) {
    Map<String, Object> m = chunk.metadata() == null ? Map.of() : chunk.metadata();
    String docId =
        Optional.ofNullable(m.get("document_id"))
            .map(String::valueOf)
            .map(String::trim)
            .filter(StringUtils::hasText)
            .orElse("unknown");
    String fn = chunk.documentTitle() == null ? "" : chunk.documentTitle();
    return new ChunkMeta(docId, fn, chunk.chunkIndex());
  }

  private String[] parseNormalizedQaJson(String raw) throws Exception {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("empty model output");
    }
    String trimmed = raw.strip();
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start < 0 || end <= start) {
      throw new IllegalArgumentException("no JSON object in output");
    }
    String json = trimmed.substring(start, end + 1);
    JsonNode root = objectMapper.readTree(json);
    JsonNode obj = root;
    if (root.isArray()) {
      if (root.size() != 1 || !root.get(0).isObject()) {
        throw new IllegalArgumentException("expected single JSON object or one-element array");
      }
      obj = root.get(0);
    }
    if (!obj.isObject()) {
      throw new IllegalArgumentException("expected JSON object");
    }
    String q = findKeyInsensitive(obj, "question");
    String a = findKeyInsensitive(obj, "answer");
    if (q == null || a == null) {
      for (String field : obj.propertyNames()) {
        JsonNode v = obj.get(field);
        if (v != null && v.isObject()) {
          q = findKeyInsensitive(v, "question");
          a = findKeyInsensitive(v, "answer");
          if (q != null && a != null) {
            break;
          }
        }
      }
    }
    if (q == null || a == null) {
      throw new IllegalArgumentException("missing question/answer in JSON");
    }
    if (!q.isBlank() && !a.isBlank()) {
      return new String[] {q, a};
    }
    throw new IllegalArgumentException("empty question or answer");
  }

  private static String findKeyInsensitive(JsonNode obj, String wanted) {
    for (String k : obj.propertyNames()) {
      if (k != null && k.equalsIgnoreCase(wanted)) {
        JsonNode v = obj.get(k);
        return v == null || v.isNull() ? null : v.asText();
      }
    }
    return null;
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

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max);
  }

  private record ChunkMeta(String documentId, String filename, Integer chunkIndex) {
    String chunkKey() {
      return documentId + ":" + (chunkIndex == null ? -1 : chunkIndex);
    }
  }
}
