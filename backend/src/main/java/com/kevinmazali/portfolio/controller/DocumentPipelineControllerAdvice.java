package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.controller.advice.ApiErrorCorrelation;
import com.kevinmazali.portfolio.model.ApiError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Maps ingestion / I/O failures on admin document routes to 503 with {@link ApiError}.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DocumentPipelineController.class)
@RequiredArgsConstructor
public class DocumentPipelineControllerAdvice {

  /** User-facing text; internal causes are logged only. */
  private static final String PIPELINE_UNAVAILABLE =
      "Document service is temporarily unavailable. Please try again later.";

  private final ApiErrorCorrelation correlation;

  /** Validation errors from admin document routes (bad filters, JSON, model id). */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest()
        .body(correlation.apply(new ApiError(ex.getMessage())));
  }

  /** Misconfiguration or unexpected empty state from the ingestion service. */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
    log.error("Pipeline illegal state: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(correlation.apply(new ApiError(PIPELINE_UNAVAILABLE, "PIPELINE_UNAVAILABLE")));
  }

  /** Low-level I/O (timeouts, connection refused). */
  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<ApiError> resourceAccess(ResourceAccessException ex) {
    log.warn("Pipeline resource access failure: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(correlation.apply(new ApiError(PIPELINE_UNAVAILABLE, "PIPELINE_UNAVAILABLE")));
  }

  /** Network or HTTP errors from downstream services used by the admin pipeline. */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ApiError> restClient(RestClientException ex) {
    log.warn("Pipeline REST client failure: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(correlation.apply(new ApiError(PIPELINE_UNAVAILABLE, "PIPELINE_UNAVAILABLE")));
  }
}
