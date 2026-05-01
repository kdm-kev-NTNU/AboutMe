package com.kevinmazali.portfolio.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetExceededExceptionTest {

  @Test
  void messageIsPreserved() {
    BudgetExceededException ex = new BudgetExceededException("daily cap reached");
    assertEquals("daily cap reached", ex.getMessage());
  }
}
