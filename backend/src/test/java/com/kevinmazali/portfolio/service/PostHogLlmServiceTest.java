package com.kevinmazali.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PostHogLlmServiceTest {

  @Test
  void disabledClient_skipsCaptureWithoutError() {
    PostHogProperties props = new PostHogProperties();
    props.setEnabled(false);

    PostHogLlmService service = new PostHogLlmService(props, null);

    assertFalse(service.isEnabled());
    assertDoesNotThrow(
        () ->
            service.captureGenerationAsync(
                "user-1",
                "gpt-test",
                1,
                2,
                BigDecimal.ZERO,
                true,
                0.1,
                "test_span",
                AiGenerationAnalytics.empty()));
  }
}
