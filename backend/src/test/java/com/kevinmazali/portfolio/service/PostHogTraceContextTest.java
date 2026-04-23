package com.kevinmazali.portfolio.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostHogTraceContextTest {

  private final PostHogTraceContext ctx = new PostHogTraceContext();

  @AfterEach
  void tearDown() {
    ctx.clear();
  }

  @Test
  void beginTrace_setsRootAndUsesDefaultTraceNameWhenNull() {
    ctx.beginTrace(null, "conv-1");
    assertEquals("trace", ctx.traceName());
    assertEquals("conv-1", ctx.conversationId());
    assertNotNull(ctx.rootTraceId());
    ctx.setExperimentRunId(99L);
    assertEquals(99L, ctx.getExperimentRunId());
  }

  @Test
  void setExperimentRunId_noOpWhenNoTrace() {
    ctx.setExperimentRunId(1L);
    assertNull(ctx.getExperimentRunId());
  }

  @Test
  void startSpan_withoutBeginTrace_returnsSyntheticRootEqualToSpan() {
    PostHogTraceContext.ActiveSpan span = ctx.startSpan();
    assertNotNull(span.spanId());
    assertEquals(span.rootTraceId(), span.spanId());
    assertNull(span.parentSpanId());
    ctx.endSpan();
  }

  @Test
  void nestedSpans_popOnEndSpan() {
    ctx.beginTrace("eval", null);
    PostHogTraceContext.ActiveSpan outer = ctx.startSpan();
    PostHogTraceContext.ActiveSpan inner = ctx.startSpan();
    assertEquals(outer.spanId(), inner.parentSpanId());
    ctx.endSpan();
    ctx.endSpan();
    PostHogTraceContext.ActiveSpan again = ctx.startSpan();
    assertNull(again.parentSpanId());
  }

  @Test
  void clear_removesState() {
    ctx.beginTrace("t", "c");
    ctx.startSpan();
    ctx.clear();
    assertNull(ctx.rootTraceId());
    ctx.endSpan();
  }

  @Test
  void gettersReturnNullWhenCleared() {
    ctx.beginTrace("x", null);
    assertNotNull(ctx.rootTraceId());
    ctx.clear();
    assertNull(ctx.rootTraceId());
    assertNull(ctx.traceName());
  }

  @Test
  void endSpan_whenStackEmpty_isNoOp() {
    ctx.beginTrace("t", null);
    ctx.endSpan();
    assertTrue(ctx.traceName().length() > 0);
  }
}
