package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server-side PostHog capture for LLM analytics ({@code $ai_generation} events). Separate from
 * frontend {@code VITE_POSTHOG_*}; use the same project API key or a dedicated backend project.
 */
@ConfigurationProperties(prefix = "portfolio.posthog")
public class PostHogProperties {

  private boolean enabled = false;

  /** PostHog project API key (same as frontend key if using one project). */
  private String apiKey = "";

  /** Ingest host, e.g. {@code https://eu.i.posthog.com}. */
  private String host = "https://eu.i.posthog.com";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public boolean isCaptureConfigured() {
    return enabled && apiKey != null && !apiKey.isBlank();
  }
}
