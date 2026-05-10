package com.kevinmazali.portfolio.controller.advice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

class GlobalApiExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
    validatorFactory.afterPropertiesSet();

    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    when(tracerProvider.getIfAvailable()).thenReturn(null);

    ApiErrorCorrelation correlation = new ApiErrorCorrelation(tracerProvider);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingProbe())
            .setControllerAdvice(
                new GlobalApiExceptionHandler(), new ApiErrorBodyAdvice(correlation))
            .setValidator(validatorFactory)
            .build();
  }

  @Test
  void budgetExceededMapsTo429() throws Exception {
    mockMvc
        .perform(get("/__probe/budget").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("daily cap"))
        .andExpect(jsonPath("$.code").value("BUDGET_EXCEEDED"));
  }

  @Test
  void circuitOpenMapsTo503() throws Exception {
    mockMvc
        .perform(get("/__probe/circuit").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("circuit"))
        .andExpect(jsonPath("$.code").value("CIRCUIT_OPEN"));
  }

  @Test
  void premiumForbiddenMapsTo403() throws Exception {
    mockMvc
        .perform(get("/__probe/premium").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("premium"))
        .andExpect(jsonPath("$.code").value("PREMIUM_MODEL_FORBIDDEN"));
  }

  @Test
  void unexpectedExceptionMapsTo500WithStructuredBody() throws Exception {
    mockMvc
        .perform(get("/__probe/boom").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please try again."))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    // traceId/timestamp enrichment comes from ApiErrorBodyAdvice in a full DispatcherServlet
    // (not applied under standalone MockMvc).
  }

  @Test
  void unexpectedRuntimeExceptionAlsoMapsTo500() throws Exception {
    mockMvc
        .perform(get("/__probe/runtime").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please try again."))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
  }

  @Test
  void methodArgumentNotValid_returnsViolations() throws Exception {
    mockMvc
        .perform(
            post("/__probe/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.violations").isArray())
        .andExpect(jsonPath("$.violations[0].field").value("name"));
  }

  @Test
  void unreadableJson_returnsInvalidJson() throws Exception {
    mockMvc
        .perform(
            post("/__probe/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_JSON"));
  }

  @Test
  void missingRequiredParameter_returnsMissingParameterCode() throws Exception {
    mockMvc
        .perform(get("/__probe/q-required").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
        .andExpect(jsonPath("$.error").value("Missing required parameter: q"));
  }

  @Test
  void typeMismatch_returnsTypeMismatchCode() throws Exception {
    mockMvc
        .perform(get("/__probe/int-id").param("id", "nan").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
  }

  @Test
  void responseStatusException_blankReasonUsesGenericMessage() throws Exception {
    mockMvc
        .perform(get("/__probe/rs-blank").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Request rejected"));
  }

  @Test
  void responseStatusException_includesReasonWhenPresent() throws Exception {
    mockMvc
        .perform(get("/__probe/rs-reason").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("gone"));
  }

  @Controller
  static class ThrowingProbe {

    public record ValidNameBody(@NotBlank String name) {}

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

    @GetMapping("/__probe/boom")
    void boom() throws Exception {
      throw new Exception("kaboom");
    }

    @GetMapping("/__probe/runtime")
    void runtime() {
      throw new IllegalStateException("oops");
    }

    @PostMapping(value = "/__probe/valid", consumes = MediaType.APPLICATION_JSON_VALUE)
    void validBody(@RequestBody @Valid ValidNameBody body) {}

    @GetMapping("/__probe/q-required")
    void qRequired(@RequestParam String q) {}

    @GetMapping("/__probe/int-id")
    void intId(@RequestParam int id) {}

    @GetMapping("/__probe/rs-blank")
    void rsBlank() {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/__probe/rs-reason")
    void rsReason() {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "gone");
    }
  }
}
