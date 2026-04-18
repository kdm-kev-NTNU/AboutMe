package com.kevinmazali.portfolio.controller;

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

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
  }

  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ApiError> restClient(RestClientException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
  }
}
