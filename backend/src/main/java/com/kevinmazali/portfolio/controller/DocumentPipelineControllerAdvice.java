package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Maps ingestion / I/O failures on admin document routes to 503 with {@link ApiError}.
 */
@RestControllerAdvice(assignableTypes = DocumentPipelineController.class)
@RequiredArgsConstructor
public class DocumentPipelineControllerAdvice {

  /** Validation errors from admin document routes (bad filters, JSON, model id). */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
  }

  /** Misconfiguration or unexpected empty state from the ingestion service. */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
  }

  /** Low-level I/O (timeouts, connection refused). */
  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<ApiError> resourceAccess(ResourceAccessException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ApiError(ex.getMessage()));
  }

  /** Network or HTTP errors from downstream services used by the admin pipeline. */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ApiError> restClient(RestClientException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ApiError(ex.getMessage()));
  }
}
