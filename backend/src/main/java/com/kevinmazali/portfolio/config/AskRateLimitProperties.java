package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable rate limits for {@code POST /ask} (Layer 2).
 */
@ConfigurationProperties(prefix = "portfolio.ask-rate-limit")
public class AskRateLimitProperties {

  private boolean enabled = true;

  /** Authenticated users: max requests per window. */
  private int authenticatedCapacity = 5;

  /** Authenticated users: window length in seconds. */
  private int authenticatedWindowSeconds = 10;

  /** Anonymous: max requests per window (stricter tier). */
  private int anonymousCapacity = 3;

  /** Anonymous: window length in seconds. */
  private int anonymousWindowSeconds = 10;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getAuthenticatedCapacity() {
    return authenticatedCapacity;
  }

  public void setAuthenticatedCapacity(int authenticatedCapacity) {
    this.authenticatedCapacity = authenticatedCapacity;
  }

  public int getAuthenticatedWindowSeconds() {
    return authenticatedWindowSeconds;
  }

  public void setAuthenticatedWindowSeconds(int authenticatedWindowSeconds) {
    this.authenticatedWindowSeconds = authenticatedWindowSeconds;
  }

  public int getAnonymousCapacity() {
    return anonymousCapacity;
  }

  public void setAnonymousCapacity(int anonymousCapacity) {
    this.anonymousCapacity = anonymousCapacity;
  }

  public int getAnonymousWindowSeconds() {
    return anonymousWindowSeconds;
  }

  public void setAnonymousWindowSeconds(int anonymousWindowSeconds) {
    this.anonymousWindowSeconds = anonymousWindowSeconds;
  }
}
