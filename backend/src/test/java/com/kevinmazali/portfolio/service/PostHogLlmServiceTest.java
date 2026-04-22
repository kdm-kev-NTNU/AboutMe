package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PostHogProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PostHogLlmServiceTest {

  @Test
  void disabledWhenNotConfigured() {
    PostHogProperties p = new PostHogProperties();
    PostHogLlmService svc = new PostHogLlmService(p, null);
    assertFalse(svc.isEnabled());
    assertDoesNotThrow(
        () ->
            svc.captureGenerationAsync(
                "user:test", "gpt-5.4-mini", 1, 2, BigDecimal.ZERO, false, 0.1, "unit"));
    svc.shutdown();
  }

  @Test
  void enabledWithBlankKeyDoesNotCreateClient() {
    PostHogProperties p = new PostHogProperties();
    p.setEnabled(true);
    p.setApiKey("   ");
    PostHogLlmService svc = new PostHogLlmService(p, null);
    assertFalse(svc.isEnabled());
    svc.shutdown();
  }
}
