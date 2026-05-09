package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flag and defaults for OpenAI Realtime (WebRTC) voice sessions.
 */
@ConfigurationProperties(prefix = "portfolio.realtime")
public class RealtimeProperties {

  /** When false, voice endpoints return 503 and the SPA should hide the feature. */
  private boolean enabled = false;

  private String model = "gpt-realtime-2";

  private String voice = "marin";

  /** Reasoning effort for GPT-Realtime-2: minimal, low, medium, high, xhigh. */
  private String reasoningEffort = "low";

  private int maxResponseOutputTokens = 1024;

  /**
   * Estimated audio input tokens recorded against budget when a session is minted (actual usage is on OpenAI;
   * this is a conservative reservation).
   */
  private int reservationInputTokens = 2000;

  private int reservationOutputTokens = 2000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getVoice() {
    return voice;
  }

  public void setVoice(String voice) {
    this.voice = voice;
  }

  public String getReasoningEffort() {
    return reasoningEffort;
  }

  public void setReasoningEffort(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }

  public int getMaxResponseOutputTokens() {
    return maxResponseOutputTokens;
  }

  public void setMaxResponseOutputTokens(int maxResponseOutputTokens) {
    this.maxResponseOutputTokens = maxResponseOutputTokens;
  }

  public int getReservationInputTokens() {
    return reservationInputTokens;
  }

  public void setReservationInputTokens(int reservationInputTokens) {
    this.reservationInputTokens = reservationInputTokens;
  }

  public int getReservationOutputTokens() {
    return reservationOutputTokens;
  }

  public void setReservationOutputTokens(int reservationOutputTokens) {
    this.reservationOutputTokens = reservationOutputTokens;
  }
}
