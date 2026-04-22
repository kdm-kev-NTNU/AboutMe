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

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiBudgetServiceTest {

  @Mock
  private AiUsageRepository usageRepository;

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
    service = new AiBudgetService(properties, usageRepository, new SimpleMeterRegistry(), null);
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
    service.recordUsage("user:a", "gpt-5.4-mini", 1000, 500, false);
    verify(usageRepository).save(any());
  }

  @Test
  void systemUserSkipsBudgetCheck() {
    assertDoesNotThrow(() -> service.assertWithinBudget(AiBudgetService.systemEvaluatorUserId(), false));
  }
}
