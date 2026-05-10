package com.kevinmazali.portfolio.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.model.ApiError;
import io.micrometer.tracing.Tracer;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;

class ApiErrorBodyAdviceTest {

  @SuppressWarnings("unchecked")
  private static ObjectProvider<Tracer> nullTracerProvider() {
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);
    return tracerProvider;
  }

  @Test
  void resolvesToApiError_trueForResponseEntityApiError() throws Exception {
    Method m = Samples.class.getDeclaredMethod("responseEntityApiError");
    MethodParameter mp = new MethodParameter(m, -1);
    assertThat(ApiErrorBodyAdvice.resolvesToApiError(mp)).isTrue();
  }

  @Test
  void resolvesToApiError_trueForBareApiError() throws Exception {
    Method m = Samples.class.getDeclaredMethod("bareApiError");
    MethodParameter mp = new MethodParameter(m, -1);
    assertThat(ApiErrorBodyAdvice.resolvesToApiError(mp)).isTrue();
  }

  @Test
  void resolvesToApiError_falseForStringBody() throws Exception {
    Method m = Samples.class.getDeclaredMethod("stringBody");
    MethodParameter mp = new MethodParameter(m, -1);
    assertThat(ApiErrorBodyAdvice.resolvesToApiError(mp)).isFalse();
  }

  @Test
  void resolvesToApiError_falseForResponseEntityString() throws Exception {
    Method m = Samples.class.getDeclaredMethod("responseEntityString");
    MethodParameter mp = new MethodParameter(m, -1);
    assertThat(ApiErrorBodyAdvice.resolvesToApiError(mp)).isFalse();
  }

  @Test
  void beforeBodyWrite_appliesCorrelationToApiError() throws Exception {
    ApiErrorCorrelation correlation = new ApiErrorCorrelation(nullTracerProvider());
    ApiErrorBodyAdvice advice = new ApiErrorBodyAdvice(correlation);

    Method m = Samples.class.getDeclaredMethod("bareApiError");
    MethodParameter mp = new MethodParameter(m, -1);

    ApiError raw = new ApiError("x", "Y");
    Object out = advice.beforeBodyWrite(raw, mp, null, null, null, null);

    assertThat(out).isInstanceOf(ApiError.class);
    ApiError apiError = (ApiError) out;
    assertThat(apiError.timestamp()).isNotNull();
  }

  @Test
  void beforeBodyWrite_passesThroughNonApiError() throws Exception {
    ApiErrorCorrelation correlation = new ApiErrorCorrelation(nullTracerProvider());
    ApiErrorBodyAdvice advice = new ApiErrorBodyAdvice(correlation);

    Method m = Samples.class.getDeclaredMethod("stringBody");
    MethodParameter mp = new MethodParameter(m, -1);

    assertThat(advice.beforeBodyWrite("plain", mp, null, null, null, null)).isEqualTo("plain");
  }

  private interface Samples {
    ResponseEntity<ApiError> responseEntityApiError();

    ApiError bareApiError();

    String stringBody();

    ResponseEntity<String> responseEntityString();
  }
}
