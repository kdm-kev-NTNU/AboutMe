package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-user / anonymous spend limits and model pricing (Layer 3).
 */
@ConfigurationProperties(prefix = "portfolio.ai.budget")
public class AiBudgetProperties {

  private boolean enabled = true;

  private BigDecimal dailyLimitUsd = new BigDecimal("2.00");
  private BigDecimal monthlyLimitUsd = new BigDecimal("20.00");
  private BigDecimal anonymousDailyLimitUsd = new BigDecimal("0.50");
  private BigDecimal anonymousMonthlyLimitUsd = new BigDecimal("5.00");

  /** Log WARN when hourly spend exceeds this multiple of (dailyLimit / 24). */
  private double spikeHourlyMultiplier = 5.0;

  /** Salt for hashing anonymous client IPs into stable budget keys. */
  private String anonIdentitySalt = "portfolio-ai-budget";

  /** Per-model pricing: USD per 1M input / output tokens (defaults filled in yaml). */
  private Map<String, ModelPricing> models = new HashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public BigDecimal getDailyLimitUsd() {
    return dailyLimitUsd;
  }

  public void setDailyLimitUsd(BigDecimal dailyLimitUsd) {
    this.dailyLimitUsd = dailyLimitUsd;
  }

  public BigDecimal getMonthlyLimitUsd() {
    return monthlyLimitUsd;
  }

  public void setMonthlyLimitUsd(BigDecimal monthlyLimitUsd) {
    this.monthlyLimitUsd = monthlyLimitUsd;
  }

  public BigDecimal getAnonymousDailyLimitUsd() {
    return anonymousDailyLimitUsd;
  }

  public void setAnonymousDailyLimitUsd(BigDecimal anonymousDailyLimitUsd) {
    this.anonymousDailyLimitUsd = anonymousDailyLimitUsd;
  }

  public BigDecimal getAnonymousMonthlyLimitUsd() {
    return anonymousMonthlyLimitUsd;
  }

  public void setAnonymousMonthlyLimitUsd(BigDecimal anonymousMonthlyLimitUsd) {
    this.anonymousMonthlyLimitUsd = anonymousMonthlyLimitUsd;
  }

  public double getSpikeHourlyMultiplier() {
    return spikeHourlyMultiplier;
  }

  public void setSpikeHourlyMultiplier(double spikeHourlyMultiplier) {
    this.spikeHourlyMultiplier = spikeHourlyMultiplier;
  }

  public String getAnonIdentitySalt() {
    return anonIdentitySalt;
  }

  public void setAnonIdentitySalt(String anonIdentitySalt) {
    this.anonIdentitySalt = anonIdentitySalt;
  }

  public Map<String, ModelPricing> getModels() {
    return models;
  }

  public void setModels(Map<String, ModelPricing> models) {
    this.models = models != null ? models : new HashMap<>();
  }

  /** Nested pricing for a single model id (Spring maps yaml keys to this). */
  public static class ModelPricing {
    private BigDecimal inputPerMillionUsd = BigDecimal.ZERO;
    private BigDecimal outputPerMillionUsd = BigDecimal.ZERO;

    public BigDecimal getInputPerMillionUsd() {
      return inputPerMillionUsd;
    }

    public void setInputPerMillionUsd(BigDecimal inputPerMillionUsd) {
      this.inputPerMillionUsd = inputPerMillionUsd;
    }

    public BigDecimal getOutputPerMillionUsd() {
      return outputPerMillionUsd;
    }

    public void setOutputPerMillionUsd(BigDecimal outputPerMillionUsd) {
      this.outputPerMillionUsd = outputPerMillionUsd;
    }
  }
}
