package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limit for {@code POST /admin/tools/experiments/run} (Layer 2).
 */
@ConfigurationProperties(prefix = "portfolio.experiments.run-rate-limit")
public class ExperimentRunRateLimitProperties {

  private boolean enabled = true;

  private int capacity = 10;

  private int windowSeconds = 60;

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
