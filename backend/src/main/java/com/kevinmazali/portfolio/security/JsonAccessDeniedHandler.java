package com.kevinmazali.portfolio.security;

import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.model.ApiError;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** JSON {@link ApiError} for 403 on secured routes. */
public final class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;
  private final ObjectProvider<Tracer> tracer;

  public JsonAccessDeniedHandler(ObjectMapper objectMapper, ObjectProvider<Tracer> tracer) {
    this.objectMapper = objectMapper;
    this.tracer = tracer;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    ApiError body = new ApiError("Access denied", "FORBIDDEN").withCorrelation(traceId(), timestamp());
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private String traceId() {
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

  private static String timestamp() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
  }
}
