package com.kevinmazali.portfolio.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiCircuitOpenExceptionTest {

  @Test
  void messageIsPreserved() {
    AiCircuitOpenException ex = new AiCircuitOpenException("circuit open");
    assertEquals("circuit open", ex.getMessage());
  }
}
