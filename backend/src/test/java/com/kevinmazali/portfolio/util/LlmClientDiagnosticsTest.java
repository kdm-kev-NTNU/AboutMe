package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmClientDiagnosticsTest {

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
