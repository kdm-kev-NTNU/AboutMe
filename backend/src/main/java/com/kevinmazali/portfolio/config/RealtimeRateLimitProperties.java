package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token bucket for {@code POST /realtime/session} (per IP).
 */
@ConfigurationProperties(prefix = "portfolio.realtime-rate-limit")
public class RealtimeRateLimitProperties {

  private boolean enabled = true;

  /** Max POST /realtime/session calls per window per IP. */
  private int capacity = 3;

  /** Refill window in seconds (e.g. 3600 = hourly). */
  private int windowSeconds = 3600;

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
