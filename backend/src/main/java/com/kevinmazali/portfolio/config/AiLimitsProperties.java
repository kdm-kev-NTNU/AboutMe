package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable hard caps for LLM chat, judge calls, and text I/O (Layer 1).
 */
@ConfigurationProperties(prefix = "portfolio.ai.limits")
public class AiLimitsProperties {

  /** Max completion tokens for RAG / public chat. */
  private int chatMaxCompletionTokens = 400;

  /** Max tokens for LLM-as-judge evaluators. */
  private int judgeMaxTokens = 512;

  /** Max question length (characters). */
  private int maxQuestionChars = 3000;

  /** Max feedback message length (characters). */
  private int maxFeedbackChars = 4000;

  /** Defensive max assistant output length after provider response (characters). */
  private int maxOutputChars = 4000;

  public int getChatMaxCompletionTokens() {
    return chatMaxCompletionTokens;
  }

  public void setChatMaxCompletionTokens(int chatMaxCompletionTokens) {
    this.chatMaxCompletionTokens = chatMaxCompletionTokens;
  }

  public int getJudgeMaxTokens() {
    return judgeMaxTokens;
  }

  public void setJudgeMaxTokens(int judgeMaxTokens) {
    this.judgeMaxTokens = judgeMaxTokens;
  }

  public int getMaxQuestionChars() {
    return maxQuestionChars;
  }

  public void setMaxQuestionChars(int maxQuestionChars) {
    this.maxQuestionChars = maxQuestionChars;
  }

  public int getMaxFeedbackChars() {
    return maxFeedbackChars;
  }

  public void setMaxFeedbackChars(int maxFeedbackChars) {
    this.maxFeedbackChars = maxFeedbackChars;
  }

  public int getMaxOutputChars() {
    return maxOutputChars;
  }

  public void setMaxOutputChars(int maxOutputChars) {
    this.maxOutputChars = maxOutputChars;
  }
}
