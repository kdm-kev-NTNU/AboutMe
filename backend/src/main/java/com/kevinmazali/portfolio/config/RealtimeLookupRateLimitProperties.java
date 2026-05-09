package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token bucket for {@code POST /realtime/lookup} (per IP).
 */
@ConfigurationProperties(prefix = "portfolio.realtime-lookup-rate-limit")
public class RealtimeLookupRateLimitProperties {

  private boolean enabled = true;

  /** Max POST /realtime/lookup calls per window per IP. */
  private int capacity = 200;

  /** Refill window in seconds. */
  private int windowSeconds = 1800;

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
