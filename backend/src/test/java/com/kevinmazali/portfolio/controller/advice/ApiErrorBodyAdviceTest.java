package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.model.ApiError;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiErrorBodyAdviceTest {

  @Test
  void supportsApiErrorReturnTypesButNotOthers() throws Exception {
    ApiErrorBodyAdvice advice = new ApiErrorBodyAdvice(new ApiErrorCorrelation(mock(org.springframework.beans.factory.ObjectProvider.class)));

    assertTrue(advice.supports(returnType("apiError"), null));
    assertTrue(advice.supports(returnType("responseEntityApiError"), null));
    assertFalse(advice.supports(returnType("plainString"), null));
    assertFalse(advice.supports(returnType("responseEntityString"), null));
  }

  @Test
  void beforeBodyWriteEnrichesOnlyApiErrorBodies() throws Exception {
    @SuppressWarnings("unchecked")
    org.springframework.beans.factory.ObjectProvider<io.micrometer.tracing.Tracer> tracerProvider =
        mock(org.springframework.beans.factory.ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    ApiErrorBodyAdvice advice = new ApiErrorBodyAdvice(correlation);

    ApiError enriched = (ApiError) advice.beforeBodyWrite(
        new ApiError("boom"),
        returnType("apiError"),
        null,
        null,
        mock(ServerHttpRequest.class),
        mock(ServerHttpResponse.class));
    assertEquals("boom", enriched.error());
    assertTrue(enriched.traceId() == null || !enriched.traceId().isBlank());

    String plain = (String) advice.beforeBodyWrite(
        "ok",
        returnType("plainString"),
        null,
        null,
        mock(ServerHttpRequest.class),
        mock(ServerHttpResponse.class));
    assertEquals("ok", plain);
  }

  private static MethodParameter returnType(String methodName) throws Exception {
    Method method = Probe.class.getDeclaredMethod(methodName);
    return new MethodParameter(method, -1);
  }

  @SuppressWarnings("unused")
  static class Probe {
    ApiError apiError() {
      return new ApiError("boom");
    }

    ResponseEntity<ApiError> responseEntityApiError() {
      return ResponseEntity.ok(new ApiError("boom"));
    }

    String plainString() {
      return "ok";
    }

    ResponseEntity<String> responseEntityString() {
      return ResponseEntity.ok("ok");
    }
  }
}
