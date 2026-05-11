package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.model.ApiError;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiErrorCorrelationTest {

  @Test
  void apply_keepsExistingCorrelationValues() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    ApiError existing = new ApiError("boom", "CODE", List.of()).withCorrelation("trace-1", "2026-01-01T00:00:00Z");

    ApiError result = correlation.apply(existing);

    assertEquals("trace-1", result.traceId());
    assertEquals("2026-01-01T00:00:00Z", result.timestamp());
  }

  @Test
  void currentTraceId_returnsNullWhenTracerMissingOrSpanMissing() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    assertNull(correlation.currentTraceId());

    Tracer tracer = mock(Tracer.class);
    when(tracer.currentSpan()).thenReturn(null);
    when(tracerProvider.getIfAvailable()).thenReturn(tracer);
    assertNull(correlation.currentTraceId());
  }

  @Test
  void currentTraceId_returnsTraceIdWhenSpanContextAvailable() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    Tracer tracer = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext context = mock(TraceContext.class);
    when(tracerProvider.getIfAvailable()).thenReturn(tracer);
    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(context);
    when(context.traceId()).thenReturn("abc123");

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);

    assertEquals("abc123", correlation.currentTraceId());
    ApiError result = correlation.apply(new ApiError("boom"));
    assertEquals("abc123", result.traceId());
    assertNotNull(result.timestamp());
  }
}
