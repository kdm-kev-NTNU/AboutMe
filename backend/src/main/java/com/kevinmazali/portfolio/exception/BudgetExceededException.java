package com.kevinmazali.portfolio.exception;

/**
 * Thrown when a user exceeds configured daily or monthly AI spend (Layer 3).
 */
public class BudgetExceededException extends RuntimeException {

  public BudgetExceededException(String message) {
    super(message);
  }
}
