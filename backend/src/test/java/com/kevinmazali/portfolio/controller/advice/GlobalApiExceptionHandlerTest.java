package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalApiExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingProbe())
            .setControllerAdvice(new GlobalApiExceptionHandler())
            .build();
  }

  @Test
  void budgetExceededMapsTo429() throws Exception {
    mockMvc
        .perform(get("/__probe/budget").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("daily cap"));
  }

  @Test
  void circuitOpenMapsTo503() throws Exception {
    mockMvc
        .perform(get("/__probe/circuit").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("circuit"));
  }

  @Test
  void premiumForbiddenMapsTo403() throws Exception {
    mockMvc
        .perform(get("/__probe/premium").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("premium"));
  }

  @Controller
  static class ThrowingProbe {

    @GetMapping("/__probe/budget")
    void budget() {
      throw new BudgetExceededException("daily cap");
    }

    @GetMapping("/__probe/circuit")
    void circuit() {
      throw new AiCircuitOpenException("circuit");
    }

    @GetMapping("/__probe/premium")
    void premium() {
      throw new PremiumModelForbiddenException("premium");
    }
  }
}
