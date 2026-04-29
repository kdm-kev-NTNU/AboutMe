package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.posthog.server.PostHog;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostHogLlmServiceTest {

  @Test
  void disabledWhenNotConfigured() {
    PostHogProperties p = new PostHogProperties();
    PostHogLlmService svc = new PostHogLlmService(p, null);
    assertFalse(svc.isEnabled());
    assertDoesNotThrow(
        () ->
            svc.captureGenerationAsync(
                "user:test",
                "gpt-5.4-mini",
                1,
                2,
                BigDecimal.ZERO,
                false,
                0.1,
                "unit",
                AiGenerationAnalytics.empty()));
    svc.shutdown();
  }

  @Test
  void enabledWithBlankKeyDoesNotCreateClient() {
    PostHogProperties p = new PostHogProperties();
    p.setEnabled(true);
    p.setApiKey("   ");
    PostHogLlmService svc = new PostHogLlmService(p, null);
    assertFalse(svc.isEnabled());
    svc.shutdown();
  }

  @Test
  void captureGenerationSync_sendsAnthropicProviderAndOptionalFields() {
    PostHogInterface client = mock(PostHogInterface.class);
    PostHogProperties p = configuredProperties();
    String longInput = "x".repeat(12_001);
    String longCtx = "c".repeat(48_001);
    Map<String, Object> extra = new HashMap<>();
    extra.put("k1", "v1");
    extra.put("k2", null);
    extra.put(null, "ignored");
    AiGenerationAnalytics analytics =
        AiGenerationAnalytics.empty()
            .withTexts(longInput, "out", longCtx)
            .withTrace("span-1", "parent-1", null, "trace-n", "conv-1")
            .withError(true, "e".repeat(5000), 503)
            .withExperimentProps(extra);

    try (MockedStatic<PostHog> ph = mockStatic(PostHog.class)) {
      ph.when(() -> PostHog.with(any(PostHogConfig.class))).thenReturn(client);
      PostHogLlmService svc = new PostHogLlmService(p, null);
      assertTrue(svc.isEnabled());
      svc.captureGenerationSync(
          " ",
          "claude-3-opus",
          -1,
          -2,
          new BigDecimal("-0.01"),
          false,
          -1.0,
          "rag",
          analytics);
      verify(client).capture(eq("unknown"), eq("$ai_generation"), any());
      svc.shutdown();
    }
  }

  @Test
  void captureGenerationSync_resolvesTraceFromTracerWhenZeroTraceId() {
    PostHogInterface client = mock(PostHogInterface.class);
    Tracer tracer = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext ctx = mock(TraceContext.class);
    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(ctx);
    when(ctx.traceId()).thenReturn("00000000000000000000000000000000");

    try (MockedStatic<PostHog> ph = mockStatic(PostHog.class)) {
      ph.when(() -> PostHog.with(any(PostHogConfig.class))).thenReturn(client);
      PostHogLlmService svc = new PostHogLlmService(configuredProperties(), tracer);
      svc.captureGenerationSync(
          "system:job",
          null,
          1,
          1,
          BigDecimal.ONE,
          false,
          0.5,
          "ok",
          AiGenerationAnalytics.empty()
              .withTrace(null, null, null, null, null)
              .withExperimentProps(Map.of()));
      verify(client).capture(eq("system:job"), eq("$ai_generation"), any());
      svc.shutdown();
    }
  }

  @Test
  void shutdown_swallowsCloseException() {
    PostHogInterface client = mock(PostHogInterface.class);
    doThrow(new RuntimeException("close failed")).when(client).close();
    try (MockedStatic<PostHog> ph = mockStatic(PostHog.class)) {
      ph.when(() -> PostHog.with(any(PostHogConfig.class))).thenReturn(client);
      PostHogLlmService svc = new PostHogLlmService(configuredProperties(), null);
      assertDoesNotThrow(svc::shutdown);
    }
  }

  @Test
  void captureGenerationSync_includesBaseUrlWhenSet() {
    PostHogInterface client = mock(PostHogInterface.class);
    AiGenerationAnalytics analytics =
        new AiGenerationAnalytics(
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
            "https://api.openai.com/v1",
            false,
            null,
            null,
            Collections.emptyMap());
    try (MockedStatic<PostHog> ph = mockStatic(PostHog.class)) {
      ph.when(() -> PostHog.with(any(PostHogConfig.class))).thenReturn(client);
      PostHogLlmService svc = new PostHogLlmService(configuredProperties(), null);
      svc.captureGenerationSync(
          "u1", "gpt-5", 0, 0, BigDecimal.ZERO, false, null, null, analytics);
      verify(client).capture(eq("u1"), eq("$ai_generation"), any());
      svc.shutdown();
    }
  }

  private static PostHogProperties configuredProperties() {
    PostHogProperties p = new PostHogProperties();
    p.setEnabled(true);
    p.setApiKey("phc_test");
    p.setHost("https://eu.i.posthog.com");
    return p;
  }
}
