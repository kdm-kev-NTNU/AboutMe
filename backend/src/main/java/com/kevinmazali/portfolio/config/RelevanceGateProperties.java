package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pre-RAG relevance gate: lightweight keyword/pattern checks before vector search and LLM calls.
 */
@ConfigurationProperties(prefix = "portfolio.relevance-gate")
public class RelevanceGateProperties {

  private boolean enabled = true;

  /**
   * When true, longer queries with no in-scope signals are treated as off-topic (still allows short
   * ambiguous queries such as skill names).
   */
  private boolean strictMode = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isStrictMode() {
    return strictMode;
  }

  public void setStrictMode(boolean strictMode) {
    this.strictMode = strictMode;
  }
}
