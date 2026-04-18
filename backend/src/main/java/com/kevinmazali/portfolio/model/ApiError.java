package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple JSON error envelope")
public record ApiError(
    @Schema(description = "Human-readable error message", example = "Question cannot be empty")
    String error
) {
}
