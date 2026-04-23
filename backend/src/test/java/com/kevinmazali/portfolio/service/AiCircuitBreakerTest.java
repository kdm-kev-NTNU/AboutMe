package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCircuitBreakerTest {

  @Mock
  private AiUsageRepository usageRepository;

  private AiKillSwitchProperties properties;
  private AiCircuitBreaker breaker;

  @BeforeEach
  void setUp() {
    properties = new AiKillSwitchProperties();
    properties.setEnabled(true);
    properties.setMonthlyLimitUsd(new BigDecimal("100.00"));
    breaker = new AiCircuitBreaker(properties, usageRepository, new SimpleMeterRegistry());
  }

  @Test
  void assertClosedPassesWhenSpendBelowLimit() {
    when(usageRepository.sumGlobalCostSince(org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("1.00"));
    breaker.refreshAutoStateFromSpend();
    assertDoesNotThrow(breaker::assertClosed);
    assertFalse(breaker.isOpen());
  }

  @Test
  void refreshTripsWhenSpendAtOrAboveLimit() {
    when(usageRepository.sumGlobalCostSince(org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("100.00"));
    breaker.refreshAutoStateFromSpend();
    assertTrue(breaker.isAutoKillOpen());
    assertThrows(AiCircuitOpenException.class, breaker::assertClosed);
  }

  @Test
  void adminKillOpensCircuit() {
    breaker.setAdminKillOpen(true);
    assertThrows(AiCircuitOpenException.class, breaker::assertClosed);
  }

  @Test
  void whenKillSwitchDisabledNothingTripsAndAssertClosedIsNoOp() {
    properties.setEnabled(false);
    breaker = new AiCircuitBreaker(properties, usageRepository, new SimpleMeterRegistry());
    breaker.setAdminKillOpen(true);
    assertFalse(breaker.isOpen());
    assertDoesNotThrow(breaker::assertClosed);
    breaker.refreshAutoStateFromSpend();
    assertFalse(breaker.isAutoKillOpen());
  }

  @Test
  void refreshWithNoMonthlyLimitOnlyUpdatesSpend() {
    properties.setMonthlyLimitUsd(null);
    when(usageRepository.sumGlobalCostSince(org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("999"));
    breaker.refreshAutoStateFromSpend();
    assertFalse(breaker.isAutoKillOpen());
    assertEquals(0, breaker.getLastKnownMonthSpendUsd().compareTo(new BigDecimal("999")));
  }

  @Test
  void refreshLogsWarnBetweenWarnFractionAndLimit() {
    properties.setWarnFraction(0.5);
    properties.setMonthlyLimitUsd(new BigDecimal("100"));
    when(usageRepository.sumGlobalCostSince(org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("60"));
    breaker.refreshAutoStateFromSpend();
    assertFalse(breaker.isAutoKillOpen());
  }

  @Test
  void clearAutoTripClearsFlag() {
    when(usageRepository.sumGlobalCostSince(org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("100"));
    breaker.refreshAutoStateFromSpend();
    assertTrue(breaker.isAutoKillOpen());
    breaker.clearAutoTrip();
    assertFalse(breaker.isAutoKillOpen());
  }
}
