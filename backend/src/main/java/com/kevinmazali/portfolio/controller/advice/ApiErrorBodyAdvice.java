package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.model.ApiError;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Ensures {@link ApiError} bodies include trace id and timestamp when produced by MVC handlers.
 * Registered as a {@link org.springframework.context.annotation.Bean} via {@link com.kevinmazali.portfolio.config.ApiErrorConfiguration}
 * (not {@code @ControllerAdvice}) so constructor wiring works consistently in sliced tests.
 */
public class ApiErrorBodyAdvice implements ResponseBodyAdvice<Object> {

  private final ApiErrorCorrelation correlation;

  public ApiErrorBodyAdvice(ApiErrorCorrelation correlation) {
    this.correlation = correlation;
  }

  @Override
  public boolean supports(
      MethodParameter returnType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return resolvesToApiError(returnType);
  }

  static boolean resolvesToApiError(MethodParameter returnType) {
    Type gen = returnType.getGenericParameterType();
    if (gen instanceof ParameterizedType pt) {
      Type raw = pt.getRawType();
      if (raw instanceof Class<?> rawClass
          && org.springframework.http.ResponseEntity.class.isAssignableFrom(rawClass)) {
        Type[] args = pt.getActualTypeArguments();
        if (args.length > 0 && args[0] instanceof Class<?> bodyClass) {
          return ApiError.class.isAssignableFrom(bodyClass);
        }
      }
    }
    Class<?> param = returnType.getParameterType();
    return ApiError.class.isAssignableFrom(param);
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ApiError apiError) {
      return correlation.apply(apiError);
    }
    return body;
  }
}
