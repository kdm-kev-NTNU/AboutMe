package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Admin voice-interview defaults for Realtime input transcription and post-save cleaning.
 */
@ConfigurationProperties(prefix = "portfolio.interview")
public class InterviewProperties {

  /** OpenAI Realtime input audio transcription model. */
  private String transcriptionModel = "gpt-4o-transcribe";

  /** Chat model used to normalize cleaned interview transcripts for ingest. */
  private String cleanModel = "gpt-5.4-mini";

  private int cleanMaxTokens = 8192;

  public String getTranscriptionModel() {
    return transcriptionModel;
  }

  public void setTranscriptionModel(String transcriptionModel) {
    this.transcriptionModel = transcriptionModel;
  }

  public String getCleanModel() {
    return cleanModel;
  }

  public void setCleanModel(String cleanModel) {
    this.cleanModel = cleanModel;
  }

  public int getCleanMaxTokens() {
    return cleanMaxTokens;
  }

  public void setCleanMaxTokens(int cleanMaxTokens) {
    this.cleanMaxTokens = cleanMaxTokens;
  }

  public String resolvedTranscriptionModel() {
    return StringUtils.hasText(transcriptionModel) ? transcriptionModel.trim() : "gpt-4o-transcribe";
  }

  public String resolvedCleanModel() {
    return StringUtils.hasText(cleanModel) ? cleanModel.trim() : "gpt-5.4-mini";
  }

  public int resolvedCleanMaxTokens() {
    return cleanMaxTokens > 0 ? cleanMaxTokens : 8192;
  }
}
