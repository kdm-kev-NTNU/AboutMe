package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.config.RetrievalProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.RagAnswer;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.security.AnalyticsIdentityService;
import com.kevinmazali.portfolio.util.AiRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Default implementation of {@link OpenAIService} that performs RAG:
 * expands the query, retrieves similar documents, builds a prompt, and invokes
 * an OpenAI or Anthropic chat model depending on the selected allow-listed model.
 */
@Slf4j
@Service
public class OpenAIServiceImpl implements OpenAIService {

  private final ObjectProvider<OpenAiChatModel> openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final VectorStore vectorStore;
  private final String defaultModelId;
  private final PromptVersionService promptVersionService;
  private final AiLimitsProperties aiLimitsProperties;
  private final AiBudgetProperties aiBudgetProperties;
  private final AiBudgetService aiBudgetService;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final DocumentReranker documentReranker;
  private final RetrievalProperties retrievalProperties;
  private final PostHogTraceContext postHogTraceContext;
  @org.springframework.lang.Nullable
  private final PostHogFeatureFlagService postHogFeatureFlagService;
  @org.springframework.lang.Nullable
  private final PostHogLlmService postHogLlmService;
  private final AnalyticsIdentityService analyticsIdentityService;

  public OpenAIServiceImpl(
      ObjectProvider<OpenAiChatModel> openAiChatModel,
      ObjectProvider<AnthropicChatModel> anthropicChatModel,
      @Lazy VectorStore vectorStore,
      @Value("${portfolio.chat.default-model-id}") String defaultModelId,
      PromptVersionService promptVersionService,
      AiLimitsProperties aiLimitsProperties,
      AiBudgetProperties aiBudgetProperties,
      AiBudgetService aiBudgetService,
      AiCircuitBreaker aiCircuitBreaker,
      DocumentReranker documentReranker,
      RetrievalProperties retrievalProperties,
      PostHogTraceContext postHogTraceContext,
      AnalyticsIdentityService analyticsIdentityService,
      @Autowired(required = false) @org.springframework.lang.Nullable PostHogFeatureFlagService postHogFeatureFlagService,
      @Autowired(required = false) @org.springframework.lang.Nullable PostHogLlmService postHogLlmService) {
    this.openAiChatModel = openAiChatModel;
    this.anthropicChatModel = anthropicChatModel;
    this.vectorStore = vectorStore;
    this.defaultModelId = defaultModelId;
    this.promptVersionService = promptVersionService;
    this.aiLimitsProperties = aiLimitsProperties;
    this.aiBudgetProperties = aiBudgetProperties;
    this.aiBudgetService = aiBudgetService;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.documentReranker = documentReranker;
    this.retrievalProperties = retrievalProperties;
    this.postHogTraceContext = postHogTraceContext;
    this.analyticsIdentityService = analyticsIdentityService;
    this.postHogFeatureFlagService = postHogFeatureFlagService;
    this.postHogLlmService = postHogLlmService;
  }

  @Override
  public Answer getAnswer(Question question) {
    RagAnswer rag = getAnswerWithDocuments(question);
    return new Answer(rag.answer());
  }

  @Override
  public RagAnswer getAnswerWithDocuments(Question question) {
    SupportedChatModel model = resolveModel(question);
    if (model.provider() == ChatProvider.OPENAI && openAiChatModel.getIfAvailable() == null) {
      throw new IllegalStateException("OpenAI chat is not available (missing API key, OPENAI_CHAT_ENABLED=false, or autoconfiguration).");
    }
    if (model.provider() == ChatProvider.ANTHROPIC && anthropicChatModel.getIfAvailable() == null) {
      throw new IllegalStateException("Anthropic chat is not available (missing API key or autoconfiguration).");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(aiBudgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();

    String conversationId = question.conversationId();
    if (conversationId == null || conversationId.isBlank()) {
      conversationId = "rag:" + UUID.randomUUID();
    }
    postHogTraceContext.beginTrace("rag_ask", conversationId);
    try {
      return getAnswerWithDocumentsInner(question, model, budgetUserId, anonymous);
    } finally {
      postHogTraceContext.clear();
    }
  }

  private RagAnswer getAnswerWithDocumentsInner(
      Question question, SupportedChatModel model, String budgetUserId, boolean anonymous) {

    long traceStartNs = System.nanoTime();
    boolean traceFailed = false;
    String traceErrorMsg = null;
    try {
      Map<String, Object> phFeatureProps = Collections.emptyMap();
      PostHogFeatureFlagService ffSvc = postHogFeatureFlagService;
      AnalyticsIdentityService.PostHogCaptureIdentity phIdentity =
          analyticsIdentityService.captureIdentity(budgetUserId, anonymous);
      if (ffSvc != null && ffSvc.isEnabled()) {
        phFeatureProps = ffSvc.resolveForDistinctId(phIdentity.distinctId());
      }

      List<String> queries =
          expandQueryToLanguages(question.question(), model, budgetUserId, anonymous, phFeatureProps);

      int vectorTopK = Math.max(1, retrievalProperties.getVectorTopK());
      int candidateCap = Math.max(1, retrievalProperties.getCandidateLimit());
      int contextTopK = Math.max(1, retrievalProperties.getContextTopK());

      long retrievalStartNs = System.nanoTime();
      String retrievalSpanId = UUID.randomUUID().toString();

      List<Document> merged = queries.stream()
          .flatMap(q -> {
            try {
              String queryText = q == null ? "" : q;
              List<Document> results = vectorStore.similaritySearch(
                  SearchRequest.builder()
                      .query(queryText)
                      .topK(vectorTopK)
                      .build()
              );
              return results.stream();
            } catch (Exception ex) {
              log.warn("similaritySearch failed for a query variant: {}", ex.getMessage());
              return Stream.empty();
            }
          })
          .toList();

      List<Document> candidates = dedupePreserveOrder(merged).stream()
          .limit(candidateCap)
          .toList();

      List<Document> documents =
          documentReranker.rerank(question.question(), candidates, contextTopK);

      double retrievalLatencySec = (System.nanoTime() - retrievalStartNs) / 1_000_000_000.0;
      captureRagRetrievalSpanIfEnabled(phIdentity, retrievalSpanId, retrievalLatencySec);

      log.debug(
          "RAG retrieval: merged={} dedupCapped={} contextTopK={} finalDocs={}",
          merged.size(),
          candidates.size(),
          contextTopK,
          documents.size());

      List<String> contentList = documents.stream().map(Document::getText).toList();
      String providerName = switch (model.provider()) {
        case OPENAI -> "openai";
        case ANTHROPIC -> "anthropic";
      };
      String ragPromptTemplate = promptVersionService.loadRagPrompt(providerName);
      PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
      Prompt basePrompt = promptTemplate.create(Map.of(
          "input", question.question(),
          "documents", String.join("\n", contentList)
      ));

      String joinedContext = String.join("\n---\n", contentList);
      ChatResponse response =
          invokeManagedChat(
              model,
              basePrompt,
              budgetUserId,
              anonymous,
              "rag_completion",
              joinedContext,
              question.question(),
              documents.size(),
              phFeatureProps);
      String answerText = response.getResult().getOutput().getText();
      answerText = truncateOutput(answerText, aiLimitsProperties.getMaxOutputChars());
      return new RagAnswer(answerText, contentList);
    } catch (RuntimeException e) {
      traceFailed = true;
      traceErrorMsg = e.getMessage();
      throw e;
    } finally {
      captureRagTraceSummaryIfEnabled(
          phIdentity, traceStartNs, traceFailed, traceErrorMsg);
    }
  }

  private void captureRagRetrievalSpanIfEnabled(
      AnalyticsIdentityService.PostHogCaptureIdentity phIdentity,
      String retrievalSpanId,
      double latencySec) {
    PostHogLlmService ph = postHogLlmService;
    if (ph == null || !ph.isEnabled()) {
      return;
    }
    String traceId = postHogTraceContext.rootTraceId();
    String sessionId = postHogTraceContext.conversationId();
    if (traceId == null || traceId.isBlank()) {
      return;
    }
    ph.captureSpanAsync(
        phIdentity.distinctId(),
        traceId,
        sessionId,
        retrievalSpanId,
        traceId,
        "rag_retrieval",
        latencySec,
        false,
        phIdentity.anonymous());
  }

  private void captureRagTraceSummaryIfEnabled(
      AnalyticsIdentityService.PostHogCaptureIdentity phIdentity,
      long traceStartNs,
      boolean failed,
      @org.springframework.lang.Nullable String errorMessage) {
    PostHogLlmService ph = postHogLlmService;
    if (ph == null || !ph.isEnabled()) {
      return;
    }
    String traceId = postHogTraceContext.rootTraceId();
    if (traceId == null || traceId.isBlank()) {
      return;
    }
    double totalSec = (System.nanoTime() - traceStartNs) / 1_000_000_000.0;
    ph.captureTraceAsync(
        phIdentity.distinctId(),
        traceId,
        postHogTraceContext.conversationId(),
        postHogTraceContext.traceName(),
        totalSec,
        failed,
        errorMessage,
        phIdentity.anonymous());
  }

  private static List<Document> dedupePreserveOrder(List<Document> merged) {
    LinkedHashMap<String, Document> byKey = new LinkedHashMap<>();
    for (Document d : merged) {
      byKey.putIfAbsent(documentDedupeKey(d), d);
    }
    return List.copyOf(byKey.values());
  }

  private static String documentDedupeKey(Document d) {
    if (d.getId() != null && !d.getId().isBlank()) {
      return d.getId();
    }
    String t = d.getText();
    return t == null ? "" : t;
  }

  private SupportedChatModel resolveModel(Question question) {
    String id = (question.model() == null || question.model().isBlank())
        ? defaultModelId
        : question.model().trim();
    SupportedChatModel model = SupportedChatModel.fromModelId(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown or unsupported model id: " + id));
    if (AiRequestContext.isAnonymousInteractiveUser() && model.requiresAuthenticationForPublicChat()) {
      throw new PremiumModelForbiddenException(
          "This model requires sign-in. Use a public model or authenticate.");
    }
    return model;
  }

  private ChatResponse invokeManagedChat(
      SupportedChatModel model,
      Prompt basePrompt,
      String budgetUserId,
      boolean anonymous,
      String generationSpanName,
      @org.springframework.lang.Nullable String posthogContextDocuments,
      @org.springframework.lang.Nullable String userQuestion,
      @org.springframework.lang.Nullable Integer contextDocumentCount,
      Map<String, Object> posthogFeatureProps) {
    PostHogTraceContext.ActiveSpan span = postHogTraceContext.startSpan();
    try {
      aiCircuitBreaker.assertClosed();
      aiBudgetService.assertWithinBudget(budgetUserId, anonymous);
      long startNs = System.nanoTime();
      ChatResponse response = invokeChat(model, basePrompt);
      double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
      UsageTokens usage = extractUsage(response);
      String outputText = response.getResult().getOutput().getText();
      Map<String, Object> flags =
          posthogFeatureProps != null && !posthogFeatureProps.isEmpty()
              ? posthogFeatureProps
              : Map.of();
      AiGenerationAnalytics analytics =
          AiGenerationAnalytics.empty()
              .withTrace(
                  span.spanId(),
                  span.parentSpanId(),
                  span.rootTraceId(),
                  span.traceName(),
                  span.conversationId())
              .withUserQuestion(userQuestion)
              .withInputMessages(
                  promptToInputMessages(basePrompt),
                  outputText,
                  "rag_completion".equals(generationSpanName) ? posthogContextDocuments : null)
              .withContextDocumentCount(contextDocumentCount)
              .withExperimentProps(flags);
      aiBudgetService.recordUsage(
          budgetUserId,
          model.modelId(),
          usage.promptTokens(),
          usage.completionTokens(),
          anonymous,
          latencySec,
          generationSpanName,
          analytics);
      return response;
    } finally {
      postHogTraceContext.endSpan();
    }
  }

  private ChatResponse invokeChat(SupportedChatModel model, Prompt basePrompt) {
    int maxTokens = aiLimitsProperties.getChatMaxCompletionTokens();
    return switch (model.provider()) {
      case OPENAI -> {
        OpenAiChatModel openAi = openAiChatModel.getIfAvailable();
        if (openAi == null) {
          throw new IllegalStateException("OpenAI chat is not available (missing API key, OPENAI_CHAT_ENABLED=false, or autoconfiguration).");
        }
        OpenAiChatOptions opts = OpenAiChatOptions.builder()
            .model(model.modelId())
            .maxCompletionTokens(maxTokens)
            .build();
        yield openAi.call(new Prompt(basePrompt.getInstructions(), opts));
      }
      case ANTHROPIC -> {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
          throw new IllegalStateException("Anthropic chat is not available (missing API key or autoconfiguration).");
        }
        AnthropicChatOptions opts = AnthropicChatOptions.builder()
            .model(model.modelId())
            .maxTokens(maxTokens)
            .build();
        yield anthropic.call(new Prompt(basePrompt.getInstructions(), opts));
      }
    };
  }

  private List<String> expandQueryToLanguages(
      String original,
      SupportedChatModel model,
      String budgetUserId,
      boolean anonymous,
      Map<String, Object> posthogFeatureProps) {
    try {
      String sys = """
      Translate the user query into both English and Norwegian.
      Return ONLY this exact JSON object with double quotes and no extra text:
      {"en": "<english>", "no": "<norwegian>"}
      """.strip();

      Prompt base = new PromptTemplate("{sys}\nUser: {q}")
          .create(Map.of("sys", sys, "q", original));
      ChatResponse r =
          invokeManagedChat(
              model, base, budgetUserId, anonymous, "query_expansion", null, original, null, posthogFeatureProps);
      String json = r.getResult().getOutput().getText();

      String en = extractJsonValue(json, "en");
      String no = extractJsonValue(json, "no");

      return List.of(original,
          en == null || en.isBlank() ? original : en,
          no == null || no.isBlank() ? original : no);
    } catch (BudgetExceededException | AiCircuitOpenException | PremiumModelForbiddenException e) {
      throw e;
    } catch (Exception e) {
      return List.of(original);
    }
  }

  private String extractJsonValue(String json, String key) {
    try {
      String marker = "\"" + key + "\"" + ":";
      int i = json.indexOf(marker);
      if (i < 0) {
        return null;
      }
      int start = json.indexOf('"', i + marker.length());
      if (start < 0) {
        return null;
      }
      int end = json.indexOf('"', start + 1);
      if (end < 0) {
        return null;
      }
      return json.substring(start + 1, end);
    } catch (Exception ex) {
      return null;
    }
  }

  private static List<Map<String, String>> promptToInputMessages(Prompt prompt) {
    List<Message> messages = prompt.getInstructions();
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }
    List<Map<String, String>> rows = new ArrayList<>();
    for (Message m : messages) {
      if (m instanceof UserMessage um) {
        rows.add(Map.of("role", "user", "content", textOrEmpty(um.getText())));
      } else if (m instanceof SystemMessage sm) {
        rows.add(Map.of("role", "system", "content", textOrEmpty(sm.getText())));
      } else if (m instanceof AssistantMessage am) {
        rows.add(Map.of("role", "assistant", "content", textOrEmpty(am.getText())));
      } else if (m != null) {
        rows.add(Map.of("role", "user", "content", m.toString()));
      }
    }
    return List.copyOf(rows);
  }

  private static String textOrEmpty(@org.springframework.lang.Nullable String t) {
    return t != null ? t : "";
  }

  private static String truncateOutput(String text, int maxChars) {
    if (text == null) {
      return "";
    }
    if (text.length() <= maxChars) {
      return text;
    }
    return text.substring(0, maxChars);
  }

  private static UsageTokens extractUsage(ChatResponse response) {
    if (response == null || response.getMetadata() == null) {
      return new UsageTokens(0, 0);
    }
    Usage usage = response.getMetadata().getUsage();
    if (usage == null) {
      return new UsageTokens(0, 0);
    }
    int prompt = safeInt(usage.getPromptTokens());
    int completion = safeInt(usage.getCompletionTokens());
    return new UsageTokens(prompt, completion);
  }

  private static int safeInt(Number v) {
    if (v == null) {
      return 0;
    }
    long x = v.longValue();
    if (x > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (x < 0) {
      return 0;
    }
    return (int) x;
  }

  private record UsageTokens(int promptTokens, int completionTokens) {
  }
}
