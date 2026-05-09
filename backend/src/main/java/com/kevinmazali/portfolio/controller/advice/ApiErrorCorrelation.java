package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.model.ApiError;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.ObjectProvider;

/** Adds trace id and timestamp to {@link ApiError} bodies for support correlation. */
public class ApiErrorCorrelation {

  private final ObjectProvider<Tracer> tracer;

  public ApiErrorCorrelation(ObjectProvider<Tracer> tracer) {
    this.tracer = tracer;
  }

  public ApiError apply(ApiError error) {
    if (error.traceId() != null && error.timestamp() != null) {
      return error;
    }
    String traceId = currentTraceId();
    String ts = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    return error.withCorrelation(traceId, ts);
  }

  public String currentTraceId() {
    Tracer t = tracer.getIfAvailable();
    if (t == null) {
      return null;
    }
    var span = t.currentSpan();
    if (span != null && span.context() != null) {
      return span.context().traceId();
    }
    return null;
  }
}
