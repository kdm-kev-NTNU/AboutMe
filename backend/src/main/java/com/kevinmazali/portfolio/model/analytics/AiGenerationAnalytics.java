package com.kevinmazali.portfolio.model.analytics;

import java.util.Collections;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Optional fields sent with {@code $ai_generation} for PostHog LLM observability.
 */
public record AiGenerationAnalytics(
    @Nullable String userQuestion,
    @Nullable String inputText,
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

  public static AiGenerationAnalytics empty() {
    return new AiGenerationAnalytics(
        null,
        null,
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

  public AiGenerationAnalytics withTexts(
      @Nullable String inputText, @Nullable String outputText, @Nullable String contextText) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }

  public AiGenerationAnalytics withUserQuestion(@Nullable String userQuestion) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }

  public AiGenerationAnalytics withContextDocumentCount(@Nullable Integer contextDocumentCount) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }

  public AiGenerationAnalytics withError(
      boolean error, @Nullable String errorMessage, @Nullable Integer httpStatus) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }

  public AiGenerationAnalytics withTrace(
      @Nullable String spanId,
      @Nullable String parentSpanId,
      @Nullable String rootTraceId,
      @Nullable String traceName,
      @Nullable String conversationId) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }

  public AiGenerationAnalytics withExperimentProps(Map<String, Object> props) {
    return new AiGenerationAnalytics(
        userQuestion,
        inputText,
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
        inputText,
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
        experimentProperties != null ? experimentProperties : Collections.emptyMap());
  }
}
