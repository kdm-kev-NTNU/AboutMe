package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.posthog.server.PostHog;
import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Sends {@code $ai_generation} events to PostHog for LLM analytics (tokens, cost, latency, input/output, trace
 * hierarchy). Runs capture asynchronously; disabled when {@link PostHogProperties#isCaptureConfigured()} is false.
 */
@Slf4j
@Service
public class PostHogLlmService {

  private static final String ZERO_TRACE = "00000000000000000000000000000000";
  private static final int MAX_AI_BODY_CHARS = 12_000;
  private static final int MAX_AI_CONTEXT_CHARS = 48_000;

  @Nullable
  private final Tracer tracer;
  @Nullable
  private final PostHogInterface client;

  public PostHogLlmService(
      PostHogProperties properties,
      @Autowired(required = false) @Nullable Tracer tracer) {
    this.tracer = tracer;
    if (properties.isCaptureConfigured()) {
      PostHogConfig config =
          PostHogConfig.builder(properties.getApiKey().trim()).host(properties.getHost().trim()).build();
      this.client = PostHog.with(config);
    } else {
      this.client = null;
    }
  }

  public boolean isEnabled() {
    return client != null;
  }

  /**
   * Fire-and-forget {@code $ai_generation} after the DB transaction commits (caller should schedule
   * afterCommit; this method only performs async network I/O).
   */
  public void captureGenerationAsync(
      String distinctId,
      String modelId,
      int inputTokens,
      int outputTokens,
      BigDecimal totalCostUsd,
      boolean anonymous,
      @Nullable Double latencySeconds,
      @Nullable String spanName,
      AiGenerationAnalytics analytics) {
    if (client == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    AiGenerationAnalytics a = analytics != null ? analytics : AiGenerationAnalytics.empty();
    CompletableFuture.runAsync(
        () -> {
          try {
            captureGenerationSync(
                id, modelId, inputTokens, outputTokens, totalCostUsd, anonymous, latencySeconds, spanName, a);
          } catch (Exception e) {
            log.warn("PostHog $ai_generation capture failed: {}", e.getMessage());
          }
        });
  }

  void captureGenerationSync(
      String distinctId,
      String modelId,
      int inputTokens,
      int outputTokens,
      BigDecimal totalCostUsd,
      boolean anonymous,
      @Nullable Double latencySeconds,
      @Nullable String spanName,
      AiGenerationAnalytics analytics) {
    PostHogInterface ph = client;
    if (ph == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    String traceId = resolveTraceId(analytics.rootTraceId());
    String provider = providerForModel(modelId);

    PostHogCaptureOptions.Builder b = PostHogCaptureOptions.builder()
        .property("$ai_trace_id", traceId)
        .property("$ai_model", modelId != null ? modelId : "")
        .property("$ai_provider", provider)
        .property("$ai_input_tokens", Math.max(0, inputTokens))
        .property("$ai_output_tokens", Math.max(0, outputTokens));

    if (latencySeconds != null && !latencySeconds.isNaN() && latencySeconds >= 0) {
      b.property("$ai_latency", latencySeconds);
    }
    if (totalCostUsd != null && totalCostUsd.signum() >= 0) {
      b.property("$ai_total_cost_usd", totalCostUsd.doubleValue());
    }
    if (spanName != null && !spanName.isBlank()) {
      b.property("$ai_span_name", spanName);
    }
    String sid = analytics.spanId() != null && !analytics.spanId().isBlank()
        ? analytics.spanId()
        : UUID.randomUUID().toString();
    b.property("$ai_span_id", sid);
    if (analytics.parentSpanId() != null && !analytics.parentSpanId().isBlank()) {
      b.property("$ai_parent_id", analytics.parentSpanId());
    }
    if (analytics.traceName() != null && !analytics.traceName().isBlank()) {
      b.property("$ai_trace_name", analytics.traceName());
    }
    if (analytics.conversationId() != null && !analytics.conversationId().isBlank()) {
      b.property("$ai_conversation_id", analytics.conversationId());
    }
    if (analytics.inputText() != null && !analytics.inputText().isBlank()) {
      b.property("$ai_input", truncate(analytics.inputText(), MAX_AI_BODY_CHARS));
    }
    if (analytics.outputText() != null && !analytics.outputText().isBlank()) {
      b.property("$ai_output", truncate(analytics.outputText(), MAX_AI_BODY_CHARS));
    }
    if (analytics.contextText() != null && !analytics.contextText().isBlank()) {
      b.property("$ai_context", truncate(analytics.contextText(), MAX_AI_CONTEXT_CHARS));
    }
    if (analytics.baseUrl() != null && !analytics.baseUrl().isBlank()) {
      b.property("$ai_base_url", analytics.baseUrl());
    }
    b.property("$ai_is_error", analytics.error());
    if (analytics.errorMessage() != null && !analytics.errorMessage().isBlank()) {
      b.property("$ai_error", truncate(analytics.errorMessage(), 4_000));
    }
    if (analytics.httpStatus() != null) {
      b.property("$ai_http_status", analytics.httpStatus());
    }
    if (anonymous || id.startsWith("anon:") || id.startsWith("system:")) {
      b.property("$process_person_profile", false);
    }
    Map<String, Object> extra = analytics.experimentProperties();
    if (extra != null && !extra.isEmpty()) {
      for (Map.Entry<String, Object> e : extra.entrySet()) {
        if (e.getKey() != null && e.getValue() != null) {
          b.property(e.getKey(), e.getValue());
        }
      }
    }

    ph.capture(id, "$ai_generation", b.build());
  }

  private String resolveTraceId(@Nullable String explicitRoot) {
    if (explicitRoot != null && !explicitRoot.isBlank()) {
      return explicitRoot;
    }
    if (tracer == null) {
      return UUID.randomUUID().toString();
    }
    Span span = tracer.currentSpan();
    if (span == null) {
      return UUID.randomUUID().toString();
    }
    String tid = span.context().traceId();
    if (tid == null || tid.isBlank() || ZERO_TRACE.equals(tid)) {
      return UUID.randomUUID().toString();
    }
    return tid;
  }

  private static String truncate(@Nullable String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, max) + "\n...[truncated]";
  }

  private static String providerForModel(String modelId) {
    if (modelId == null) {
      return "unknown";
    }
    String m = modelId.toLowerCase();
    if (m.contains("claude")) {
      return "anthropic";
    }
    return "openai";
  }

  @PreDestroy
  public void shutdown() {
    if (client != null) {
      try {
        client.close();
      } catch (Exception e) {
        log.warn("PostHog client shutdown: {}", e.getMessage());
      }
    }
  }
}
