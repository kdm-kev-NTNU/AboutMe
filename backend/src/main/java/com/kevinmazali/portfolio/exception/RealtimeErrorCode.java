package com.kevinmazali.portfolio.exception;

/**
 * Machine-readable codes returned in {@link com.kevinmazali.portfolio.model.ApiError#code()} for
 * realtime voice and related failures.
 */
public enum RealtimeErrorCode {
  OPENAI_REJECTED,
  OPENAI_SERVER_ERROR,
  OPENAI_UNREACHABLE,
  SESSION_CONFIG_FAILED,
  API_KEY_MISSING,
  REALTIME_DISABLED,
  RATE_LIMITED,
  BUDGET_EXCEEDED,
  CIRCUIT_OPEN,
  PREMIUM_MODEL_FORBIDDEN
}
