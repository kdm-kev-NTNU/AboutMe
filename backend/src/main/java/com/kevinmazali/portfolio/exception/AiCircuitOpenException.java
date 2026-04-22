package com.kevinmazali.portfolio.exception;

/**
 * Thrown when the global AI kill switch / circuit breaker is open (Layer 4).
 */
public class AiCircuitOpenException extends RuntimeException {

  public AiCircuitOpenException(String message) {
    super(message);
  }
}
