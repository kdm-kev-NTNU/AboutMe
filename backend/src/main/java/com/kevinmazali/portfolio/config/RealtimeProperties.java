package com.kevinmazali.portfolio.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Feature flag and defaults for OpenAI Realtime (WebRTC) voice sessions.
 */
@ConfigurationProperties(prefix = "portfolio.realtime")
public class RealtimeProperties {

  public static final List<String> ALLOWED_VOICES = List.of("marin", "cedar");

  public static final List<String> ALLOWED_REASONING_EFFORTS = List.of("low", "medium", "high");

  /** When false, voice endpoints return 503 and the SPA should hide the feature. */
  private boolean enabled = false;

  private String model = "gpt-realtime-2";

  private String voice = "marin";

  /** Reasoning effort for GPT-Realtime-2. Public choices are intentionally curated. */
  private String reasoningEffort = "low";

  private int maxResponseOutputTokens = 1024;

  /**
   * Estimated audio input tokens recorded against budget when a session is minted (actual usage is on OpenAI;
   * this is a conservative reservation).
   */
  private int reservationInputTokens = 2000;

  private int reservationOutputTokens = 2000;

  private Providers providers = new Providers();

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

  public String defaultVoice() {
    return normalizeAllowed(voice, ALLOWED_VOICES, "marin");
  }

  public String defaultReasoningEffort() {
    return normalizeAllowed(reasoningEffort, ALLOWED_REASONING_EFFORTS, "low");
  }

  public String resolveVoice(String requestedVoice) {
    return normalizeAllowed(requestedVoice, ALLOWED_VOICES, defaultVoice());
  }

  public String resolveReasoningEffort(String requestedReasoningEffort) {
    return normalizeAllowed(requestedReasoningEffort, ALLOWED_REASONING_EFFORTS, defaultReasoningEffort());
  }

  public boolean isAllowedVoice(String requestedVoice) {
    return isAllowed(requestedVoice, ALLOWED_VOICES);
  }

  public boolean isAllowedReasoningEffort(String requestedReasoningEffort) {
    return isAllowed(requestedReasoningEffort, ALLOWED_REASONING_EFFORTS);
  }

  private static String normalizeAllowed(String value, List<String> allowed, String fallback) {
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return allowed.contains(normalized) ? normalized : fallback;
  }

  private static boolean isAllowed(String value, List<String> allowed) {
    if (!StringUtils.hasText(value)) {
      return true;
    }
    return allowed.contains(value.trim().toLowerCase(Locale.ROOT));
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

  public Providers getProviders() {
    return providers;
  }

  public void setProviders(Providers providers) {
    this.providers = providers != null ? providers : new Providers();
  }

  public static class Providers {
    private OpenAiProvider openai = new OpenAiProvider();

    public OpenAiProvider getOpenai() {
      return openai;
    }

    public void setOpenai(OpenAiProvider openai) {
      this.openai = openai != null ? openai : new OpenAiProvider();
    }
  }

  public static class OpenAiProvider {
    private boolean enabled = true;
    private List<OpenAiModel> models = List.of();
    private String defaultModelId = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public List<OpenAiModel> getModels() {
      return models;
    }

    public void setModels(List<OpenAiModel> models) {
      this.models = models != null ? models : List.of();
    }

    public String getDefaultModelId() {
      return defaultModelId;
    }

    public void setDefaultModelId(String defaultModelId) {
      this.defaultModelId = defaultModelId;
    }
  }

  public static class OpenAiModel {
    private String id = "";
    private String label = "";
    private boolean defaultModel = false;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public boolean isDefaultModel() {
      return defaultModel;
    }

    public void setDefaultModel(boolean defaultModel) {
      this.defaultModel = defaultModel;
    }
  }
}
