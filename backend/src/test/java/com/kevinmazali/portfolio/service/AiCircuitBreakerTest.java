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
}
