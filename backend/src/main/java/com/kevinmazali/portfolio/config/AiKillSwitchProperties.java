package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Global monthly spend circuit breaker (Layer 4).
 */
@ConfigurationProperties(prefix = "portfolio.ai.kill-switch")
public class AiKillSwitchProperties {

  private boolean enabled = true;

  private BigDecimal monthlyLimitUsd = new BigDecimal("50.00");

  private int checkIntervalSeconds = 60;

  /** Log WARN when global month spend crosses this fraction of the limit (0–1). */
  private double warnFraction = 0.80;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public BigDecimal getMonthlyLimitUsd() {
    return monthlyLimitUsd;
  }

  public void setMonthlyLimitUsd(BigDecimal monthlyLimitUsd) {
    this.monthlyLimitUsd = monthlyLimitUsd;
  }

  public int getCheckIntervalSeconds() {
    return checkIntervalSeconds;
  }

  public void setCheckIntervalSeconds(int checkIntervalSeconds) {
    this.checkIntervalSeconds = checkIntervalSeconds;
  }

  public double getWarnFraction() {
    return warnFraction;
  }

  public void setWarnFraction(double warnFraction) {
    this.warnFraction = warnFraction;
  }
}
