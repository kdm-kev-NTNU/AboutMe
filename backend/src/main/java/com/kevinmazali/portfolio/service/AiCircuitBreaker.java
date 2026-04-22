package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global monthly spend guard and optional admin kill switch (Layer 4 + Layer 5 metrics/alerts).
 */
@Slf4j
@Component
public class AiCircuitBreaker {

  private final AiKillSwitchProperties properties;
  private final AiUsageRepository usageRepository;

  private final AtomicBoolean adminKillOpen = new AtomicBoolean(false);
  private final AtomicBoolean autoKillOpen = new AtomicBoolean(false);
  private final AtomicReference<BigDecimal> lastMonthSpendUsd = new AtomicReference<>(BigDecimal.ZERO);

  public AiCircuitBreaker(
      AiKillSwitchProperties properties,
      AiUsageRepository usageRepository,
      MeterRegistry meterRegistry) {
    this.properties = properties;
    this.usageRepository = usageRepository;
    Gauge.builder("ai.circuit_breaker.open", this, cb -> cb.isOpen() ? 1.0 : 0.0)
        .description("1 if AI circuit breaker is open (requests blocked)")
        .register(meterRegistry);
    Gauge.builder("ai.spend.global_month_ratio", this, cb -> {
          BigDecimal limit = properties.getMonthlyLimitUsd();
          if (limit == null || limit.signum() <= 0) {
            return 0.0;
          }
          return lastMonthSpendUsd.get().divide(limit, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        })
        .description("Estimated global month AI spend / kill-switch monthly limit")
        .register(meterRegistry);
  }

  public void assertClosed() {
    if (!properties.isEnabled()) {
      return;
    }
    if (isOpen()) {
      throw new AiCircuitOpenException("AI is temporarily unavailable due to cost controls.");
    }
  }

  public boolean isOpen() {
    if (!properties.isEnabled()) {
      return false;
    }
    return adminKillOpen.get() || autoKillOpen.get();
  }

  public boolean isAdminKillOpen() {
    return adminKillOpen.get();
  }

  public boolean isAutoKillOpen() {
    return autoKillOpen.get();
  }

  public void setAdminKillOpen(boolean open) {
    adminKillOpen.set(open);
    log.info("AI admin kill switch set to open={}", open);
  }

  /** Clears auto trip (e.g. after admin raises limit or data correction). */
  public void clearAutoTrip() {
    autoKillOpen.set(false);
    log.info("AI auto circuit breaker cleared");
  }

  public BigDecimal getLastKnownMonthSpendUsd() {
    return lastMonthSpendUsd.get();
  }

  /**
   * Reconciles global month spend against the kill-switch limit (default: every minute).
   */
  @Scheduled(cron = "${portfolio.ai.kill-switch.check-cron:0 * * * * *}")
  public void refreshAutoStateFromSpend() {
    if (!properties.isEnabled()) {
      autoKillOpen.set(false);
      return;
    }
    Instant monthStart = Instant.now()
        .atZone(ZoneOffset.UTC)
        .with(TemporalAdjusters.firstDayOfMonth())
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();

    BigDecimal sum = usageRepository.sumGlobalCostSince(monthStart);
    lastMonthSpendUsd.set(sum);

    BigDecimal limit = properties.getMonthlyLimitUsd();
    if (limit == null || limit.signum() <= 0) {
      return;
    }
    BigDecimal warnLevel = limit.multiply(BigDecimal.valueOf(properties.getWarnFraction()));
    if (sum.compareTo(warnLevel) >= 0 && sum.compareTo(limit) < 0) {
      log.warn(
          "Global AI month spend {} USD is at or above {}% of kill-switch limit {} USD",
          sum,
          (int) (properties.getWarnFraction() * 100),
          limit);
    }

    boolean shouldTrip = sum.compareTo(limit) >= 0;
    if (shouldTrip && !autoKillOpen.get()) {
      log.error(
          "Global AI month spend {} USD exceeded kill-switch limit {} USD — opening circuit",
          sum,
          limit);
    }
    autoKillOpen.set(shouldTrip);
  }
}
