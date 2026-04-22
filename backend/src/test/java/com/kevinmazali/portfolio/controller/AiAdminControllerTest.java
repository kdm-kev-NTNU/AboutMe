package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import com.kevinmazali.portfolio.service.AiCircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiAdminController.class)
@EnableConfigurationProperties({
    AskRateLimitProperties.class,
    ExperimentRunRateLimitProperties.class,
    AiBudgetProperties.class,
    AiKillSwitchProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class })
class AiAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AiCircuitBreaker circuitBreaker;

  @MockBean
  private AiUsageRepository usageRepository;

  @Test
  @WithMockUser(roles = "ADMIN")
  void statusReturnsCircuitAndLimits() throws Exception {
    when(circuitBreaker.isOpen()).thenReturn(false);
    when(circuitBreaker.isAdminKillOpen()).thenReturn(false);
    when(circuitBreaker.isAutoKillOpen()).thenReturn(false);
    when(circuitBreaker.getLastKnownMonthSpendUsd()).thenReturn(new BigDecimal("1.23"));

    mockMvc.perform(get("/admin/tools/ai/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.circuitOpen").value(false))
        .andExpect(jsonPath("$.globalMonthSpendUsd").value(1.23));
  }

  @Test
  void statusForbiddenForAnonymous() throws Exception {
    mockMvc.perform(get("/admin/tools/ai/status"))
        .andExpect(status().isUnauthorized());
  }
}
