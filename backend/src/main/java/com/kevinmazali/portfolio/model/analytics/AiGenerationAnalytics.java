package com.kevinmazali.portfolio.model.analytics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Optional fields sent with {@code $ai_generation} for PostHog LLM observability.
 *
 * <p>{@code inputMessages} uses OpenAI-style chat objects: each map has {@code role} and {@code
 * content} strings for {@code $ai_input} in PostHog.
 */
public record AiGenerationAnalytics(
    @Nullable String userQuestion,
    List<Map<String, String>> inputMessages,
    @Nullable String outputText,
    @Nullable String contextText,
    @Nullable Integer contextDocumentCount,
    @Nullable String spanId,
    @Nullable String parentSpanId,
    @Nullable String rootTraceId,
    @Nullable String traceName,
    @Nullable String conversationId,
    @Nullable String baseUrl,
    boolean error,
    @Nullable String errorMessage,
    @Nullable Integer httpStatus,
    Map<String, Object> experimentProperties) {

  public AiGenerationAnalytics {
    inputMessages = inputMessages != null ? List.copyOf(inputMessages) : List.of();
    experimentProperties = experimentProperties != null ? experimentProperties : Collections.emptyMap();
  }

  public static AiGenerationAnalytics empty() {
    return new AiGenerationAnalytics(
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        Collections.emptyMap());
  }

  /** Single user blob (e.g. judge prompt) as one {@code user} message. */
  public AiGenerationAnalytics withTexts(
      @Nullable String inputText, @Nullable String outputText, @Nullable String contextText) {
    List<Map<String, String>> msgs =
        (inputText == null || inputText.isBlank())
            ? List.of()
            : List.of(message("user", inputText));
    return new AiGenerationAnalytics(
        userQuestion,
        msgs,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  /** Structured chat turns for PostHog {@code $ai_input}. */
  public AiGenerationAnalytics withInputMessages(
      @Nullable List<Map<String, String>> inputMessages,
      @Nullable String outputText,
      @Nullable String contextText) {
    List<Map<String, String>> safe =
        inputMessages == null || inputMessages.isEmpty()
            ? List.of()
            : List.copyOf(inputMessages);
    return new AiGenerationAnalytics(
        userQuestion,
        safe,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  private static Map<String, String> message(String role, String content) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("role", role);
    m.put("content", content != null ? content : "");
    return Collections.unmodifiableMap(m);
  }

  public AiGenerationAnalytics withUserQuestion(@Nullable String userQuestion) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  public AiGenerationAnalytics withContextDocumentCount(@Nullable Integer contextDocumentCount) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  public AiGenerationAnalytics withError(
      boolean error, @Nullable String errorMessage, @Nullable Integer httpStatus) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  public AiGenerationAnalytics withTrace(
      @Nullable String spanId,
      @Nullable String parentSpanId,
      @Nullable String rootTraceId,
      @Nullable String traceName,
      @Nullable String conversationId) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        this.error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }

  public AiGenerationAnalytics withExperimentProps(Map<String, Object> props) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        props != null ? props : Collections.emptyMap());
  }

  public AiGenerationAnalytics withBaseUrl(@Nullable String baseUrl) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputMessages,
        outputText,
        contextText,
        contextDocumentCount,
        spanId,
        parentSpanId,
        rootTraceId,
        traceName,
        conversationId,
        baseUrl,
        error,
        errorMessage,
        httpStatus,
        experimentProperties);
  }
}
