package com.kevinmazali.portfolio.controller.advice;

import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.exception.PremiumModelForbiddenException;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.model.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps AI cost-control exceptions to HTTP status codes (Layers 1, 3, 4) and provides a last-resort
 * 500 fallback for any uncaught exception.
 *
 * <p>Ordered as {@link Ordered#LOWEST_PRECEDENCE} so controller-specific advices (e.g.
 * {@code DocumentPipelineControllerAdvice}) run first and only the truly uncaught exceptions
 * reach {@link #unexpected(Exception)}.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> responseStatus(ResponseStatusException ex) {
    HttpStatusCode code = ex.getStatusCode();
    String reason = ex.getReason();
    return ResponseEntity.status(code)
        .body(new ApiError(reason != null && !reason.isBlank() ? reason : "Request rejected"));
  }

  /**
   * {@code @Valid} on JSON bodies (records with jakarta constraints) raises this before entering the controller.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> methodArgumentNotValid(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElseGet(
                () -> ex.getBindingResult().getGlobalErrors().stream()
                    .findFirst()
                    .map(err -> err.getDefaultMessage())
                    .orElse("Validation failed"));
    return ResponseEntity.badRequest().body(new ApiError(message));
  }

  @ExceptionHandler(BudgetExceededException.class)
  public ResponseEntity<ApiError> budgetExceeded(BudgetExceededException e) {
    return ResponseEntity.status(429)
        .body(new ApiError(e.getMessage(), RealtimeErrorCode.BUDGET_EXCEEDED.name()));
  }

  @ExceptionHandler(AiCircuitOpenException.class)
  public ResponseEntity<ApiError> circuitOpen(AiCircuitOpenException e) {
    return ResponseEntity.status(503)
        .body(new ApiError(e.getMessage(), RealtimeErrorCode.CIRCUIT_OPEN.name()));
  }

  @ExceptionHandler(PremiumModelForbiddenException.class)
  public ResponseEntity<ApiError> premiumForbidden(PremiumModelForbiddenException e) {
    return ResponseEntity.status(403)
        .body(new ApiError(e.getMessage(), RealtimeErrorCode.PREMIUM_MODEL_FORBIDDEN.name()));
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
