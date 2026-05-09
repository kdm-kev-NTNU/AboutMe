package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Uniform error body for 4xx/5xx JSON responses from controllers. */
@Schema(description = "JSON error envelope with optional diagnostics")
public record ApiError(
    @Schema(description = "Human-readable error message", example = "Question cannot be empty")
    String error,
    @Schema(description = "Stable machine-readable code for clients (optional)", example = "BUDGET_EXCEEDED")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String code,
    @Schema(description = "Distributed trace id for correlating with server logs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String traceId,
    @Schema(description = "ISO-8601 timestamp when the error was produced")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String timestamp,
    @Schema(description = "Field-level validation failures when applicable")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<FieldViolation> violations
) {
  public ApiError(String error) {
    this(error, null, null, null, null);
  }

  public ApiError(String error, String code) {
    this(error, code, null, null, null);
  }

  public ApiError(String error, String code, List<FieldViolation> violations) {
    this(error, code, null, null, violations == null || violations.isEmpty() ? null : List.copyOf(violations));
  }

  /** Merge correlation fields without changing message, code, or violations. */
  public ApiError withCorrelation(String traceId, String timestamp) {
    return new ApiError(
        error,
        code,
        traceId != null ? traceId : this.traceId,
        timestamp != null ? timestamp : this.timestamp,
        violations);
  }
}
