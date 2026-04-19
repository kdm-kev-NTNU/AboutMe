package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.RagAnswer;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link OpenAIService} that performs RAG:
 * expands the query, retrieves similar documents, builds a prompt, and invokes
 * an OpenAI or Anthropic chat model depending on the selected allow-listed model.
 */
@Slf4j
@Service
public class OpenAIServiceImpl implements OpenAIService {

  private static final int CHAT_MAX_TOKENS = 400;

  private final OpenAiChatModel openAiChatModel;
  private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
  private final VectorStore vectorStore;
  private final String defaultModelId;
  private final PromptVersionService promptVersionService;

  public OpenAIServiceImpl(
      OpenAiChatModel openAiChatModel,
      ObjectProvider<AnthropicChatModel> anthropicChatModel,
      @Lazy VectorStore vectorStore,
      @Value("${portfolio.chat.default-model-id}") String defaultModelId,
      PromptVersionService promptVersionService) {
    this.openAiChatModel = openAiChatModel;
    this.anthropicChatModel = anthropicChatModel;
    this.vectorStore = vectorStore;
    this.defaultModelId = defaultModelId;
    this.promptVersionService = promptVersionService;
  }

  @Override
  public Answer getAnswer(Question question) {
    RagAnswer rag = getAnswerWithDocuments(question);
    return new Answer(rag.answer());
  }

  @Override
  public RagAnswer getAnswerWithDocuments(Question question) {
    SupportedChatModel model = resolveModel(question);
    if (model.provider() == ChatProvider.ANTHROPIC && anthropicChatModel.getIfAvailable() == null) {
      throw new IllegalStateException("Anthropic chat is not available (missing API key or autoconfiguration).");
    }

    // #region agent log
    log.info("[DEBUG-b64a63] vectorStore class={}, question={}", vectorStore.getClass().getName(), question.question());
    // #endregion

    // Cross-language retrieval: run similarity search once per expanded query, then dedupe and cap.
    List<String> queries = expandQueryToLanguages(question.question(), model);

    // #region agent log
    log.info("[DEBUG-b64a63] expanded queries={}", queries);
    // #endregion

    List<Document> documents = queries.stream()
        .flatMap(q -> {
          log.info("[DEBUG-b64a63] searching query='{}' topK=40", q);
          try {
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(q)
                    .topK(40)
                    .build()
            );
            log.info("[DEBUG-b64a63] query='{}' returned {} docs", q, results.size());
            return results.stream();
          } catch (Exception ex) {
            log.error("[DEBUG-b64a63] similaritySearch FAILED for query='{}': {}", q, ex.getMessage(), ex);
            return java.util.stream.Stream.<Document>empty();
          }
        })
        .distinct()
        .limit(40)
        .toList();

    // #region agent log
    log.info("[DEBUG-b64a63] total retrieved docs={}", documents.size());
    if (!documents.isEmpty()) {
      documents.stream().limit(3).forEach(d ->
          log.info("[DEBUG-b64a63] doc snippet: {}", d.getText().substring(0, Math.min(150, d.getText().length()))));
    }
    // #endregion

    // Build RAG prompt from DB-managed template (per provider) plus flattened chunk text.
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

    // Provider-specific chat options (model id, max tokens) are applied inside invokeChat.
    ChatResponse response = invokeChat(model, basePrompt);
    return new RagAnswer(response.getResult().getOutput().getText(), contentList);
  }

  private SupportedChatModel resolveModel(Question question) {
    String id = (question.model() == null || question.model().isBlank())
        ? defaultModelId
        : question.model().trim();
    return SupportedChatModel.fromModelId(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown or unsupported model id: " + id));
  }

  private ChatResponse invokeChat(SupportedChatModel model, Prompt basePrompt) {
    return switch (model.provider()) {
      case OPENAI -> {
        OpenAiChatOptions opts = OpenAiChatOptions.builder()
            .model(model.modelId())
            .maxCompletionTokens(CHAT_MAX_TOKENS)
            .build();
        yield openAiChatModel.call(new Prompt(basePrompt.getInstructions(), opts));
      }
      case ANTHROPIC -> {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
          throw new IllegalStateException("Anthropic chat is not available (missing API key or autoconfiguration).");
        }
        AnthropicChatOptions opts = AnthropicChatOptions.builder()
            .model(model.modelId())
            .maxTokens(CHAT_MAX_TOKENS)
            .build();
        yield anthropic.call(new Prompt(basePrompt.getInstructions(), opts));
      }
    };
  }

  /**
   * Optional LLM step: asks the same model to emit EN/NO phrasing so vector search hits both languages.
   * On any failure, returns a singleton list containing only the original query (graceful degradation).
   */
  private List<String> expandQueryToLanguages(String original, SupportedChatModel model) {
    try {
      String sys = """
      Translate the user query into both English and Norwegian.
      Return ONLY this exact JSON object with double quotes and no extra text:
      {"en": "<english>", "no": "<norwegian>"}
      """.strip();

      Prompt base = new PromptTemplate("{sys}\nUser: {q}")
          .create(Map.of("sys", sys, "q", original));
      ChatResponse r = invokeChat(model, base);
      String json = r.getResult().getOutput().getText();

      String en = extractJsonValue(json, "en");
      String no = extractJsonValue(json, "no");

      return List.of(original,
          en == null || en.isBlank() ? original : en,
          no == null || no.isBlank() ? original : no);
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

}
