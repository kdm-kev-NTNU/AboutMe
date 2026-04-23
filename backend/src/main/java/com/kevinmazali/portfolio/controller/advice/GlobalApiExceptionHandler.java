package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import com.kevinmazali.portfolio.model.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps AI cost-control exceptions to HTTP status codes (Layers 1, 3, 4).
 */
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
}
