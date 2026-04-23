package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AiBudgetServiceTest {

  @Mock
  private AiUsageRepository usageRepository;

  @Mock
  private PostHogLlmService postHogLlmService;

  private AiBudgetProperties properties;
  private AiBudgetService service;

  @BeforeEach
  void setUp() {
    properties = new AiBudgetProperties();
    properties.setEnabled(true);
    properties.setDailyLimitUsd(new BigDecimal("1.00"));
    properties.setMonthlyLimitUsd(new BigDecimal("10.00"));
    properties.setAnonymousDailyLimitUsd(new BigDecimal("0.10"));
    properties.setAnonymousMonthlyLimitUsd(new BigDecimal("1.00"));
    var pricing = new AiBudgetProperties.ModelPricing();
    pricing.setInputPerMillionUsd(new BigDecimal("1"));
    pricing.setOutputPerMillionUsd(new BigDecimal("2"));
    properties.getModels().put("gpt-5.4-mini", pricing);
    service = new AiBudgetService(properties, usageRepository, new SimpleMeterRegistry(), postHogLlmService);
  }

  @Test
  void assertWithinBudgetAllowsWhenUnderLimits() {
    when(usageRepository.sumCostSince(any(), any(Instant.class))).thenReturn(new BigDecimal("0.01"));
    assertDoesNotThrow(() -> service.assertWithinBudget("user:test", false));
  }

  @Test
  void assertWithinBudgetThrowsWhenDailyExceeded() {
    when(usageRepository.sumCostSince(any(), any(Instant.class))).thenReturn(new BigDecimal("5.00"));
    assertThrows(BudgetExceededException.class, () -> service.assertWithinBudget("user:test", false));
  }

  @Test
  void recordUsagePersistsRow() {
    when(postHogLlmService.isEnabled()).thenReturn(false);
    service.recordUsage("user:a", "gpt-5.4-mini", 1000, 500, false);
    verify(usageRepository).save(any());
  }

  @Test
  void recordUsageForwardsToPostHogWhenEnabled() {
    when(postHogLlmService.isEnabled()).thenReturn(true);
    service.recordUsage("user:a", "gpt-5.4-mini", 10, 20, false, 0.42, "rag_completion");
    verify(usageRepository).save(any());
    verify(postHogLlmService, timeout(5_000))
        .captureGenerationAsync(
            eq("user:a"),
            eq("gpt-5.4-mini"),
            eq(10),
            eq(20),
            org.mockito.ArgumentMatchers.any(BigDecimal.class),
            eq(false),
            eq(0.42),
            eq("rag_completion"),
            eq(AiGenerationAnalytics.empty()));
  }

  @Test
  void systemUserSkipsBudgetCheck() {
    assertDoesNotThrow(() -> service.assertWithinBudget(AiBudgetService.systemEvaluatorUserId(), false));
  }

  @Test
  void recordUsage_postHogDeferredUntilAfterCommitWhenSynchronizationActive() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      when(postHogLlmService.isEnabled()).thenReturn(true);
      service.recordUsage("user:a", "gpt-5.4-mini", 2, 3, false);
      assertFalse(TransactionSynchronizationManager.getSynchronizations().isEmpty());
      for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
        sync.afterCommit();
      }
      verify(postHogLlmService, timeout(5_000))
          .captureGenerationAsync(
              eq("user:a"),
              eq("gpt-5.4-mini"),
              eq(2),
              eq(3),
              org.mockito.ArgumentMatchers.any(BigDecimal.class),
              eq(false),
              eq(null),
              eq(null),
              eq(AiGenerationAnalytics.empty()));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void recordUsage_detectsSpendSpikeAgainstHourlyThreshold() {
    when(postHogLlmService.isEnabled()).thenReturn(false);
    properties.setSpikeHourlyMultiplier(1.0);
    when(usageRepository.sumCostBetween(eq("user:a"), any(Instant.class), any(Instant.class)))
        .thenReturn(new BigDecimal("10.00"));
    service.recordUsage("user:a", "gpt-5.4-mini", 1, 1, false);
    verify(usageRepository).sumCostBetween(eq("user:a"), any(Instant.class), any(Instant.class));
  }

  @Test
  void recordUsage_skipsSpikeDetectionForSystemEvaluator() {
    when(postHogLlmService.isEnabled()).thenReturn(false);
    service.recordUsage(AiBudgetService.systemEvaluatorUserId(), "gpt-5.4-mini", 1, 1, false);
    verify(usageRepository).save(any());
    verify(usageRepository, never()).sumCostBetween(any(), any(Instant.class), any(Instant.class));
  }

  @Test
  void estimateCostUsd_unknownModelReturnsZero() {
    assertEquals(BigDecimal.ZERO, service.estimateCostUsd("unknown-model", 1_000_000, 1_000_000));
  }

  @Test
  void estimateCostUsd_nullRateFieldsTreatedAsZero() {
    var p = new AiBudgetProperties.ModelPricing();
    p.setInputPerMillionUsd(null);
    p.setOutputPerMillionUsd(null);
    properties.getModels().put("free-model", p);
    assertEquals(0, service.estimateCostUsd("free-model", 500, 500).signum());
  }
}
