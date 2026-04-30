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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Sends PostHog LLM analytics events: {@code $ai_generation}, {@code $ai_trace}, {@code $ai_span}. Runs capture
 * asynchronously; disabled when {@link PostHogProperties#isCaptureConfigured()} is false.
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

  /** Fire-and-forget {@code $ai_trace} (end-of-flow summary for a logical trace). */
  public void captureTraceAsync(
      String distinctId,
      String traceId,
      @Nullable String sessionId,
      @Nullable String traceName,
      double latencySeconds,
      boolean error,
      @Nullable String errorMessage,
      boolean anonymous) {
    if (client == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    CompletableFuture.runAsync(
        () -> {
          try {
            captureTraceSync(id, traceId, sessionId, traceName, latencySeconds, error, errorMessage, anonymous);
          } catch (Exception e) {
            log.warn("PostHog $ai_trace capture failed: {}", e.getMessage());
          }
        });
  }

  /** Fire-and-forget {@code $ai_span} (e.g. retrieval, tool steps). */
  public void captureSpanAsync(
      String distinctId,
      String traceId,
      @Nullable String sessionId,
      String spanId,
      @Nullable String parentSpanId,
      String spanName,
      double latencySeconds,
      boolean error,
      boolean anonymous) {
    if (client == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    CompletableFuture.runAsync(
        () -> {
          try {
            captureSpanSync(
                id, traceId, sessionId, spanId, parentSpanId, spanName, latencySeconds, error, anonymous);
          } catch (Exception e) {
            log.warn("PostHog $ai_span capture failed: {}", e.getMessage());
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

    String parentForPh = analytics.parentSpanId();
    boolean syntheticTrace =
        analytics.spanId() != null
            && analytics.rootTraceId() != null
            && analytics.spanId().equals(analytics.rootTraceId());
    if ((parentForPh == null || parentForPh.isBlank())
        && analytics.rootTraceId() != null
        && !analytics.rootTraceId().isBlank()
        && !syntheticTrace) {
      parentForPh = traceId;
    }
    if (parentForPh != null && !parentForPh.isBlank()) {
      b.property("$ai_parent_id", parentForPh);
    }
    if (analytics.conversationId() != null && !analytics.conversationId().isBlank()) {
      b.property("$ai_session_id", analytics.conversationId());
    }
    List<Map<String, Object>> inputRows = buildAiInputMessages(analytics.inputMessages());
    if (!inputRows.isEmpty()) {
      b.property("$ai_input", inputRows);
    }
    if (analytics.outputText() != null && !analytics.outputText().isBlank()) {
      String out = truncate(analytics.outputText(), MAX_AI_BODY_CHARS);
      b.property(
          "$ai_output_choices",
          List.of(outputChoiceMap("assistant", out)));
    }
    if (analytics.contextText() != null && !analytics.contextText().isBlank()) {
      b.property("$ai_context", truncate(analytics.contextText(), MAX_AI_CONTEXT_CHARS));
    }
    if (analytics.userQuestion() != null && !analytics.userQuestion().isBlank()) {
      b.property("ask_question", truncate(analytics.userQuestion(), MAX_AI_BODY_CHARS));
    }
    if (analytics.contextDocumentCount() != null) {
      b.property("rag_doc_count", Math.max(0, analytics.contextDocumentCount()));
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

  void captureTraceSync(
      String distinctId,
      String traceId,
      @Nullable String sessionId,
      @Nullable String traceName,
      double latencySeconds,
      boolean error,
      @Nullable String errorMessage,
      boolean anonymous) {
    PostHogInterface ph = client;
    if (ph == null || traceId == null || traceId.isBlank()) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    PostHogCaptureOptions.Builder b = PostHogCaptureOptions.builder()
        .property("$ai_trace_id", traceId)
        .property("$ai_span_name", traceName != null && !traceName.isBlank() ? traceName : "trace")
        .property("$ai_is_error", error);
    if (sessionId != null && !sessionId.isBlank()) {
      b.property("$ai_session_id", sessionId);
    }
    if (!Double.isNaN(latencySeconds) && latencySeconds >= 0) {
      b.property("$ai_latency", latencySeconds);
    }
    if (errorMessage != null && !errorMessage.isBlank()) {
      b.property("$ai_error", truncate(errorMessage, 4_000));
    }
    if (anonymous || id.startsWith("anon:") || id.startsWith("system:")) {
      b.property("$process_person_profile", false);
    }
    ph.capture(id, "$ai_trace", b.build());
  }

  void captureSpanSync(
      String distinctId,
      String traceId,
      @Nullable String sessionId,
      String spanId,
      @Nullable String parentSpanId,
      String spanName,
      double latencySeconds,
      boolean error,
      boolean anonymous) {
    PostHogInterface ph = client;
    if (ph == null || traceId == null || traceId.isBlank() || spanId == null || spanId.isBlank()) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    PostHogCaptureOptions.Builder b = PostHogCaptureOptions.builder()
        .property("$ai_trace_id", traceId)
        .property("$ai_span_id", spanId)
        .property("$ai_span_name", spanName != null && !spanName.isBlank() ? spanName : "span")
        .property("$ai_is_error", error);
    if (sessionId != null && !sessionId.isBlank()) {
      b.property("$ai_session_id", sessionId);
    }
    if (parentSpanId != null && !parentSpanId.isBlank()) {
      b.property("$ai_parent_id", parentSpanId);
    }
    if (!Double.isNaN(latencySeconds) && latencySeconds >= 0) {
      b.property("$ai_latency", latencySeconds);
    }
    if (anonymous || id.startsWith("anon:") || id.startsWith("system:")) {
      b.property("$process_person_profile", false);
    }
    ph.capture(id, "$ai_span", b.build());
  }

  private static Map<String, Object> outputChoiceMap(String role, String content) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("role", role);
    m.put("content", content);
    return m;
  }

  private static List<Map<String, Object>> buildAiInputMessages(List<Map<String, String>> messages) {
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    int remaining = MAX_AI_CONTEXT_CHARS;
    for (Map<String, String> m : messages) {
      if (remaining <= 0) {
        break;
      }
      String role = m.getOrDefault("role", "user");
      String content = m.getOrDefault("content", "");
      int cap = Math.min(MAX_AI_BODY_CHARS, remaining);
      String piece = truncate(content, cap);
      if (piece.length() > remaining) {
        piece = truncate(piece, remaining);
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("role", role);
      row.put("content", piece);
      rows.add(row);
      remaining -= piece.length();
    }
    return rows;
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
