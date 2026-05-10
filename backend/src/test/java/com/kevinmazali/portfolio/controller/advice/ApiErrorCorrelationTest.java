package com.kevinmazali.portfolio.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.FieldViolation;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ApiErrorCorrelationTest {

  @Test
  void apply_leavesErrorUnchangedWhenTraceAndTimestampAlreadySet() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    ApiError original =
        new ApiError("x", "CODE", "existing-trace", "2020-01-01T00:00:00Z", null);

    assertThat(correlation.apply(original)).isSameAs(original);
  }

  @Test
  void apply_addsTimestampWhenTracerAbsent() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    ApiError err = new ApiError("msg", "CODE");
    ApiError enriched = correlation.apply(err);

    assertThat(enriched.timestamp()).isNotNull().isNotBlank();
    assertThat(enriched.traceId()).isNull();
    assertThat(enriched.error()).isEqualTo("msg");
    assertThat(enriched.code()).isEqualTo("CODE");
  }

  @Test
  void apply_addsTraceIdFromTracerSpan() {
    Tracer tracer = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext ctx = mock(TraceContext.class);
    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(ctx);
    when(ctx.traceId()).thenReturn("trace-from-micrometer");

    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(tracer);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    ApiError enriched = correlation.apply(new ApiError("e", "C"));

    assertThat(enriched.traceId()).isEqualTo("trace-from-micrometer");
    assertThat(enriched.timestamp()).isNotNull();
  }

  @Test
  void currentTraceId_returnsNullWhenSpanContextMissing() {
    Tracer tracer = mock(Tracer.class);
    when(tracer.currentSpan()).thenReturn(null);

    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(tracer);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    assertThat(correlation.currentTraceId()).isNull();
  }

  @Test
  void apply_preservesViolationsWhenAddingCorrelation() {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    List<FieldViolation> violations = List.of(new FieldViolation("f", "bad"));
    ApiError err = new ApiError("validation", "VALIDATION_FAILED", violations);
    ApiError enriched = correlation.apply(err);

    assertThat(enriched.violations()).isEqualTo(violations);
  }
}
