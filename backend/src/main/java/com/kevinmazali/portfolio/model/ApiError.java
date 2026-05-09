package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/** Uniform error body for 4xx/5xx JSON responses from controllers. */
@Schema(description = "Simple JSON error envelope")
public record ApiError(
    @Schema(description = "Human-readable error message", example = "Question cannot be empty")
    String error,
    @Schema(description = "Stable machine-readable code for clients (optional)", example = "BUDGET_EXCEEDED")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String code
) {
  public ApiError(String error) {
    this(error, null);
  }
}
