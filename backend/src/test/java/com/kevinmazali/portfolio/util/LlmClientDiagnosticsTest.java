package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmClientDiagnosticsTest {

  @Test
  void describeAskFailure_nullThrowable() {
    assertEquals("unknown", LlmClientDiagnostics.describeAskFailure(null));
  }

  @Test
  void describeAskFailure_emptyHttpBodyUsesPlaceholder() {
    RestClientResponseException upstream = mock(RestClientResponseException.class);
    when(upstream.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
    when(upstream.getStatusText()).thenReturn("Unprocessable");
    when(upstream.getResponseBodyAsString(StandardCharsets.UTF_8)).thenReturn("  \n\t  ");

    assertThat(LlmClientDiagnostics.describeAskFailure(upstream)).contains("<empty>");
  }

  @Test
  void describeAskFailure_bodyReadErrorFallsBackToEmptyBody() {
    RestClientResponseException upstream = mock(RestClientResponseException.class);
    when(upstream.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(upstream.getStatusText()).thenReturn("Bad Request");
    doThrow(new RuntimeException("no body")).when(upstream).getResponseBodyAsString(StandardCharsets.UTF_8);

    assertThat(LlmClientDiagnostics.describeAskFailure(upstream)).contains("<empty>");
  }

  @Test
  void describeAskFailure_truncatesLongSingleLineBody() {
    RestClientResponseException upstream = mock(RestClientResponseException.class);
    when(upstream.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(upstream.getStatusText()).thenReturn("Bad Request");
    String longBody = "x".repeat(2000);
    when(upstream.getResponseBodyAsString(StandardCharsets.UTF_8)).thenReturn(longBody);

    String msg = LlmClientDiagnostics.describeAskFailure(upstream);
    assertThat(msg).contains("…");
    assertThat(msg.length()).isLessThan(longBody.length() + 80);
  }

  @Test
  void describeAskFailure_rootWithoutMessageUsesClassName() {
    RuntimeException inner = new RuntimeException((String) null);
    RuntimeException outer = new RuntimeException("wrap", inner);
    assertEquals("RuntimeException", LlmClientDiagnostics.describeAskFailure(outer));
  }

  @Test
  void describeAskFailureIncludesHttpStatusAndBodyWhenRestClientResponseInChain() {
    RestClientResponseException upstream = mock(RestClientResponseException.class);
    when(upstream.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(upstream.getStatusText()).thenReturn("Bad Request");
    when(upstream.getResponseBodyAsString(StandardCharsets.UTF_8))
        .thenReturn("{\"error\":{\"message\":\"Incorrect API key provided\"}}");

    RuntimeException wrapped = new RuntimeException("Error while extracting response for type …", upstream);

    assertThat(LlmClientDiagnostics.describeAskFailure(wrapped))
        .contains("upstream HTTP 400")
        .contains("Incorrect API key provided");
  }

  @Test
  void describeAskFailureFallsBackToRootCauseMessage() {
    IllegalStateException root = new IllegalStateException("Upstream timeout");
    RuntimeException outer = new RuntimeException("wrapper", root);

    assertThat(LlmClientDiagnostics.describeAskFailure(outer))
        .isEqualTo("IllegalStateException: Upstream timeout");
  }
}
