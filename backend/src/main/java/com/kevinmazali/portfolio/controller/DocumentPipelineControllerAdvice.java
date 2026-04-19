package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.exception.ChromaFeatureDisabledException;
import com.kevinmazali.portfolio.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Maps Chroma / HTTP client failures on admin document routes to 503 with {@link ApiError},
 * consistent with {@link ChromaHealthController} behavior.
 */
@RestControllerAdvice(assignableTypes = DocumentPipelineController.class)
public class DocumentPipelineControllerAdvice {

  /** Chroma disabled via {@code portfolio.chroma.enabled=false}. */
  @ExceptionHandler(ChromaFeatureDisabledException.class)
  public ResponseEntity<ApiError> chromaDisabled(ChromaFeatureDisabledException ex) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ApiError(ex.getMessage()));
  }

  /** Chroma misconfiguration or unexpected empty state from the ingestion service. */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
  }

  /** Network or HTTP errors when calling the Chroma REST API from the admin pipeline. */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ApiError> restClient(RestClientException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
  }
}
