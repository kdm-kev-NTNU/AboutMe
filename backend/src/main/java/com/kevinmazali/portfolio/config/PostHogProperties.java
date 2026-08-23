package com.kevinmazali.portfolio.config;

import java.util.ArrayList;
import java.util.List;
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

  /**
   * PostHog feature flag keys to resolve via {@code /decide} and attach to {@code $ai_generation} as
   * {@code $feature/<key>}. When empty, no decide calls are made.
   */
  private List<String> featureFlagKeys = new ArrayList<>();

  /** HTTP timeout for {@code /decide} (milliseconds). */
  private int featureFlagsTimeoutMs = 2_000;

  /**
   * Salt for deriving stable admin analytics distinct ids. Not secret — must remain stable across
   * deploys (do not reuse JWT secret or rotation would split one owner into two PostHog persons).
   */
  private String identitySalt = "aboutme-analytics-identity-v1";

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

  public List<String> getFeatureFlagKeys() {
    return featureFlagKeys;
  }

  public void setFeatureFlagKeys(List<String> featureFlagKeys) {
    this.featureFlagKeys = featureFlagKeys != null ? featureFlagKeys : new ArrayList<>();
  }

  public int getFeatureFlagsTimeoutMs() {
    return featureFlagsTimeoutMs;
  }

  public void setFeatureFlagsTimeoutMs(int featureFlagsTimeoutMs) {
    this.featureFlagsTimeoutMs = featureFlagsTimeoutMs;
  }

  public String getIdentitySalt() {
    return identitySalt;
  }

  public void setIdentitySalt(String identitySalt) {
    this.identitySalt = identitySalt;
  }

  public boolean isCaptureConfigured() {
    return enabled && apiKey != null && !apiKey.isBlank();
  }

  /** True when decide-based flags should run for RAG (same credentials as capture). */
  public boolean isFeatureFlagDecideConfigured() {
    return isCaptureConfigured() && featureFlagKeys != null && !featureFlagKeys.isEmpty();
  }
}
