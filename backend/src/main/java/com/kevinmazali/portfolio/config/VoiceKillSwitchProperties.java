package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PostHog-backed public voice kill switch ({@code aboutme_voice_kill_switch}).
 *
 * <p>Polarity: flag active + evaluates true means voice is <em>off</em> for visitors. When the flag
 * is inactive in PostHog, {@code /flags} omits the key and voice stays on (fail-open for analytics
 * outages once a last-known value exists; cold start falls back to {@code PORTFOLIO_REALTIME_ENABLED}
 * via the realtime catalog).
 */
@ConfigurationProperties(prefix = "portfolio.voice-kill-switch")
public class VoiceKillSwitchProperties {

  /** Master switch for reading/writing the PostHog flag. */
  private boolean enabled = true;

  /** PostHog feature flag key. Server allow-lists this key only for management writes. */
  private String flagKey = "aboutme_voice_kill_switch";

  /** Distinct id used for server-side {@code /flags} evaluation (not a real user). */
  private String evaluateDistinctId = "aboutme-voice-kill-switch";

  /** How often to refresh the flag from PostHog (milliseconds). */
  private long pollIntervalMs = 30_000;

  /**
   * Private PostHog API host for flag management ({@code enable}/{@code disable}), e.g.
   * {@code https://eu.posthog.com}. Distinct from the public ingest host.
   */
  private String apiHost = "https://eu.posthog.com";

  /** PostHog project id (numeric). */
  private String projectId = "";

  /**
   * Personal API key with {@code feature_flag:read} and {@code feature_flag:write}. Never expose to
   * the frontend.
   */
  private String personalApiKey = "";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getFlagKey() {
    return flagKey;
  }

  public void setFlagKey(String flagKey) {
    this.flagKey = flagKey;
  }

  public String getEvaluateDistinctId() {
    return evaluateDistinctId;
  }

  public void setEvaluateDistinctId(String evaluateDistinctId) {
    this.evaluateDistinctId = evaluateDistinctId;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public String getApiHost() {
    return apiHost;
  }

  public void setApiHost(String apiHost) {
    this.apiHost = apiHost;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getPersonalApiKey() {
    return personalApiKey;
  }

  public void setPersonalApiKey(String personalApiKey) {
    this.personalApiKey = personalApiKey;
  }

  public boolean isManagementConfigured() {
    return enabled
        && personalApiKey != null
        && !personalApiKey.isBlank()
        && projectId != null
        && !projectId.isBlank()
        && apiHost != null
        && !apiHost.isBlank()
        && flagKey != null
        && !flagKey.isBlank();
  }

  public boolean isFlagsReadConfigured() {
    return enabled && flagKey != null && !flagKey.isBlank();
  }
}
