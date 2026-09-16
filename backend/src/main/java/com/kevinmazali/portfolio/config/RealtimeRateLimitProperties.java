package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token bucket for {@code POST /realtime/session} (per IP).
 */
@ConfigurationProperties(prefix = "portfolio.realtime-rate-limit")
public class RealtimeRateLimitProperties {

  private boolean enabled = true;

  /**
   * Max POST /realtime/session calls per window per IP.
   *
   * <p>This bucket exists to stop scripted abuse, not to control spend: cost is bounded by
   * {@code portfolio.ai.budget} and the kill switch, which reserve tokens per session. Sized to
   * absorb the reconnects a real visitor makes after a denied microphone prompt or a dropped
   * network, since each retry consumes a token and the previous limit of 3/hour locked such a
   * visitor out of the feature entirely.
   */
  private int capacity = 30;

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
