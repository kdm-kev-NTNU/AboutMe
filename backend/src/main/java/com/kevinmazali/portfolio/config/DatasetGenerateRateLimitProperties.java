package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limit for {@code POST /admin/tools/experiments/datasets/generate}.
 */
@ConfigurationProperties(prefix = "portfolio.experiments.dataset-generate-rate-limit")
public class DatasetGenerateRateLimitProperties {

  private boolean enabled = true;

  private int capacity = 15;

  private int windowSeconds = 300;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int getWindowSeconds() {
    return windowSeconds;
  }

  public void setWindowSeconds(int windowSeconds) {
    this.windowSeconds = windowSeconds;
  }
}
