package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.posthog.server.PostHog;
import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Sends {@code $ai_generation} events to PostHog for LLM analytics (tokens, cost, latency). Runs capture
 * asynchronously; disabled when {@link PostHogProperties#isCaptureConfigured()} is false.
 */
@Slf4j
@Service
public class PostHogLlmService {

  private static final String ZERO_TRACE = "00000000000000000000000000000000";

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
      @Nullable String spanName) {
    if (client == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    CompletableFuture.runAsync(
        () -> {
          try {
            captureGenerationSync(id, modelId, inputTokens, outputTokens, totalCostUsd, anonymous, latencySeconds, spanName);
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
      @Nullable String spanName) {
    PostHogInterface ph = client;
    if (ph == null) {
      return;
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    String traceId = resolveTraceId();
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
      b.property("$ai_span_id", UUID.randomUUID().toString());
    }
    if (anonymous || id.startsWith("anon:") || id.startsWith("system:")) {
      b.property("$process_person_profile", false);
    }

    ph.capture(id, "$ai_generation", b.build());
  }

  private String resolveTraceId() {
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
