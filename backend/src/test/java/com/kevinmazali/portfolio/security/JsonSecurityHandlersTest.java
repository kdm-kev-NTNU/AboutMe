package com.kevinmazali.portfolio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.model.ApiError;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class JsonSecurityHandlersTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void authenticationEntryPoint_writesJsonWithoutTracer() throws Exception {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracer = mock(ObjectProvider.class);
    when(tracer.getIfAvailable()).thenReturn(null);

    JsonAuthenticationEntryPoint ep = new JsonAuthenticationEntryPoint(mapper, tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();
    ep.commence(
        new MockHttpServletRequest(),
        response,
        new BadCredentialsException("bad"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).contains("application/json");
    ApiError body = mapper.readValue(response.getContentAsByteArray(), ApiError.class);
    assertThat(body.error()).isEqualTo("Authentication required");
    assertThat(body.code()).isEqualTo("UNAUTHORIZED");
    assertThat(body.traceId()).isNull();
    assertThat(body.timestamp()).isNotNull();
  }

  @Test
  void authenticationEntryPoint_includesTraceIdWhenPresent() throws Exception {
    Tracer tracerBean = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext ctx = mock(TraceContext.class);
    when(tracerBean.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(ctx);
    when(ctx.traceId()).thenReturn("sec-trace-1");

    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracer = mock(ObjectProvider.class);
    when(tracer.getIfAvailable()).thenReturn(tracerBean);

    JsonAuthenticationEntryPoint ep = new JsonAuthenticationEntryPoint(mapper, tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();
    ep.commence(
        new MockHttpServletRequest(),
        response,
        new BadCredentialsException("bad"));

    ApiError body = mapper.readValue(response.getContentAsByteArray(), ApiError.class);
    assertThat(body.traceId()).isEqualTo("sec-trace-1");
  }

  @Test
  void accessDenied_writesJsonWithoutTracer() throws Exception {
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracer = mock(ObjectProvider.class);
    when(tracer.getIfAvailable()).thenReturn(null);

    JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(mapper, tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.handle(
        new MockHttpServletRequest(),
        response,
        new AccessDeniedException("nope"));

    assertThat(response.getStatus()).isEqualTo(403);
    ApiError body = mapper.readValue(response.getContentAsByteArray(), ApiError.class);
    assertThat(body.error()).isEqualTo("Access denied");
    assertThat(body.code()).isEqualTo("FORBIDDEN");
    assertThat(body.traceId()).isNull();
    assertThat(body.timestamp()).isNotNull();
  }

  @Test
  void accessDenied_includesTraceIdWhenPresent() throws Exception {
    Tracer tracerBean = mock(Tracer.class);
    Span span = mock(Span.class);
    TraceContext ctx = mock(TraceContext.class);
    when(tracerBean.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(ctx);
    when(ctx.traceId()).thenReturn("sec-trace-2");

    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracer = mock(ObjectProvider.class);
    when(tracer.getIfAvailable()).thenReturn(tracerBean);

    JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(mapper, tracer);
    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.handle(
        new MockHttpServletRequest(),
        response,
        new AccessDeniedException("nope"));

    ApiError body = mapper.readValue(response.getContentAsByteArray(), ApiError.class);
    assertThat(body.traceId()).isEqualTo("sec-trace-2");
  }
}
