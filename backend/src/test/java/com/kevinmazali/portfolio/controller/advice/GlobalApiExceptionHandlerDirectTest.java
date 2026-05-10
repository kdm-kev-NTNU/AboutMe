package com.kevinmazali.portfolio.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.kevinmazali.portfolio.model.ApiError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Direct invocation tests for branches that are awkward to trigger purely through MockMvc
 * (e.g. {@link MethodArgumentNotValidException} with only global errors).
 */
class GlobalApiExceptionHandlerDirectTest {

  private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

  @Test
  void responseStatusException_usesReasonWhenPresent() {
    ResponseEntity<ApiError> res =
        handler.responseStatus(new ResponseStatusException(HttpStatus.BAD_REQUEST, "nope"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(res.getBody().error()).isEqualTo("nope");
  }

  @Test
  void responseStatusException_blankReasonFallsBackToGenericMessage() {
    ResponseEntity<ApiError> res =
        handler.responseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "   "));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(res.getBody().error()).isEqualTo("Request rejected");
  }

  @Test
  void responseStatusException_nullReasonFallsBackToGenericMessage() {
    ResponseEntity<ApiError> res =
        handler.responseStatus(new ResponseStatusException(HttpStatus.CONFLICT));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().error()).isEqualTo("Request rejected");
  }

  @Test
  void methodArgumentNotValid_globalErrorsWhenNoFieldErrors() throws Exception {
    Object target = new Object();
    BeanPropertyBindingResult binding =
        new BeanPropertyBindingResult(target, "cmd");
    binding.reject("x", "global problem");

    var method =
        GlobalApiExceptionHandlerDirectTest.class.getDeclaredMethod(
            "dummyForMethodParameter", String.class);
    var mp = new org.springframework.core.MethodParameter(method, 0);

    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, binding);

    ResponseEntity<ApiError> res = handler.methodArgumentNotValid(ex);

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(res.getBody().code()).isEqualTo("VALIDATION_FAILED");
    assertThat(res.getBody().violations()).hasSize(1);
    assertThat(res.getBody().violations().get(0).field()).isEqualTo("cmd");
    assertThat(res.getBody().violations().get(0).message()).isEqualTo("global problem");
  }

  @Test
  void methodArgumentNotValid_emptyBindingUsesSummaryOnly() throws Exception {
    BeanPropertyBindingResult binding =
        new BeanPropertyBindingResult(new Object(), "cmd");

    var method =
        GlobalApiExceptionHandlerDirectTest.class.getDeclaredMethod(
            "dummyForMethodParameter", String.class);
    var mp = new org.springframework.core.MethodParameter(method, 0);

    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, binding);

    ResponseEntity<ApiError> res = handler.methodArgumentNotValid(ex);

    assertThat(res.getBody()).isNotNull();
    assertThat(res.getBody().error()).isEqualTo("Validation failed");
    assertThat(res.getBody().violations()).isNull();
  }

  @Test
  void constraintViolationException_mapsViolations() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<BadNumber>> violations = validator.validate(new BadNumber(1));
    ConstraintViolationException ex = new ConstraintViolationException(violations);

    ResponseEntity<ApiError> res = handler.constraintViolation(ex);

    assertThat(res.getBody().violations()).hasSize(1);
    assertThat(res.getBody().violations().get(0).field()).isEqualTo("n");
    assertThat(res.getBody().violations().get(0).message()).contains("5");
  }

  record BadNumber(@Min(5) int n) {}

  @Test
  void constraintViolationException_emptySetUsesGenericSummary() {
    ConstraintViolationException ex =
        new ConstraintViolationException(Collections.emptySet());

    ResponseEntity<ApiError> res = handler.constraintViolation(ex);

    assertThat(res.getBody()).isNotNull();
    assertThat(res.getBody().error()).isEqualTo("Validation failed");
    assertThat(res.getBody().violations()).isNull();
  }

  @SuppressWarnings("unused")
  private void dummyForMethodParameter(String ignored) {}
}
