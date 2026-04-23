package com.kevinmazali.portfolio.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PremiumModelForbiddenExceptionTest {

  @Test
  void messageIsPreserved() {
    PremiumModelForbiddenException ex = new PremiumModelForbiddenException("premium only");
    assertEquals("premium only", ex.getMessage());
  }
}
