package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import com.kevinmazali.portfolio.model.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps AI cost-control exceptions to HTTP status codes (Layers 1, 3, 4).
 */
@Slf4j
@RestControllerAdvice
public class GlobalApiExceptionHandler {

  @ExceptionHandler(BudgetExceededException.class)
  public ResponseEntity<ApiError> budgetExceeded(BudgetExceededException e) {
    return ResponseEntity.status(429).body(new ApiError(e.getMessage()));
  }

  @ExceptionHandler(AiCircuitOpenException.class)
  public ResponseEntity<ApiError> circuitOpen(AiCircuitOpenException e) {
    return ResponseEntity.status(503).body(new ApiError(e.getMessage()));
  }

  @ExceptionHandler(PremiumModelForbiddenException.class)
  public ResponseEntity<ApiError> premiumForbidden(PremiumModelForbiddenException e) {
    return ResponseEntity.status(403).body(new ApiError(e.getMessage()));
  }

  /**
   * Safety net: any uncaught exception (e.g. Spring AI / OpenAI transport failures, NPEs from
   * misconfigured beans) becomes a structured 500 JSON {@link ApiError} so SPA clients can render
   * a user-friendly message instead of a raw HTML error page. The original cause is logged at
   * ERROR level for ops to investigate.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception e) {
    log.error("Unhandled exception in API: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("An unexpected error occurred. Please try again."));
  }
}
