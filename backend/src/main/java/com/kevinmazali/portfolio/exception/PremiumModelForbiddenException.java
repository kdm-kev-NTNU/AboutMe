package com.kevinmazali.portfolio.exception;

/**
 * Thrown when an anonymous caller requests a model that requires authentication (Layer 1).
 */
public class PremiumModelForbiddenException extends RuntimeException {

  public PremiumModelForbiddenException(String message) {
    super(message);
  }
}
