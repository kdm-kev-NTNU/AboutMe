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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** JSON {@link ApiError} for 401 instead of an HTML/WWW-Authenticate-only response. */
public final class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;
  private final ObjectProvider<Tracer> tracer;

  public JsonAuthenticationEntryPoint(ObjectMapper objectMapper, ObjectProvider<Tracer> tracer) {
    this.objectMapper = objectMapper;
    this.tracer = tracer;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    ApiError body =
        new ApiError("Authentication required", "UNAUTHORIZED").withCorrelation(traceId(), timestamp());
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
