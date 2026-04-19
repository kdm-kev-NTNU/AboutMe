package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

class ChromaClientDiagnosticsTest {

  @Test
  void baseUrlJoinsHostAndPort() {
    assertThat(ChromaClientDiagnostics.baseUrl("http://chroma.internal", 8000))
        .isEqualTo("http://chroma.internal:8000");
  }

  @Test
  void baseUrlUsesPlaceholderWhenHostUnset() {
    assertThat(ChromaClientDiagnostics.baseUrl(null, 8100)).isEqualTo("<unset-host>:8100");
    assertThat(ChromaClientDiagnostics.baseUrl("  ", 8100)).isEqualTo("<unset-host>:8100");
  }

  @Test
  void apiErrorBodyMessageReplacesIoErrorEndingWithNull() {
    String raw = "I/O error on GET request for \"http://host:8000/api/v2/foo\": null";
    var ex = new ResourceAccessException(raw);
    String body = ChromaClientDiagnostics.apiErrorBodyMessage(ex, "http://host:8000");
    assertThat(body)
        .startsWith("Cannot reach Chroma at http://host:8000")
        .contains("Hint:")
        .doesNotEndWith(": null");
  }

  @Test
  void healthFailureMessageKeepsRuntimeExceptionWithMessage() {
    var ex = new RuntimeException("boom");
    assertThat(ChromaClientDiagnostics.healthFailureMessage(ex, "http://x:1")).isEqualTo("boom");
  }

  @Test
  void healthFailureMessageEnrichesRestClientExceptionViaApiPath() {
    var ex = new RestClientException("temporary failure talking to Chroma");
    assertThat(ChromaClientDiagnostics.healthFailureMessage(ex, "http://chroma:8000"))
        .contains("temporary failure talking to Chroma")
        .contains("Hint:");
  }

  @Test
  void healthFailureMessageReplacesNullDetailRestClientException() {
    var ex = new RestClientException("GET failed for \"http://host/v1\": null");
    assertThat(ChromaClientDiagnostics.healthFailureMessage(ex, "http://host:8000"))
        .startsWith("Cannot reach Chroma at http://host:8000")
        .contains("Hint:");
  }
}
