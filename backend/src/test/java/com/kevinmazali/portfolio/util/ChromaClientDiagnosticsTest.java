package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class ChromaClientDiagnosticsTest {

  @Test
  void baseUrlJoinsHostAndPort() {
    assertThat(ChromaClientDiagnostics.baseUrl("http://chroma.internal", 8000))
        .isEqualTo("http://chroma.internal:8000");
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
}
