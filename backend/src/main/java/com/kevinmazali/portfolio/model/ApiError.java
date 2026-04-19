package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** Uniform error body for 4xx/5xx JSON responses from controllers. */
@Schema(description = "Simple JSON error envelope")
public record ApiError(
    @Schema(description = "Human-readable error message", example = "Question cannot be empty")
    String error
) {
}
