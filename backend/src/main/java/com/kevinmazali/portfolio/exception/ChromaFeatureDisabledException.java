package com.kevinmazali.portfolio.exception;

/**
 * Thrown when Chroma-backed features are invoked while {@code portfolio.chroma.enabled=false}.
 */
public class ChromaFeatureDisabledException extends RuntimeException {

  public ChromaFeatureDisabledException(String message) {
    super(message);
  }
}
