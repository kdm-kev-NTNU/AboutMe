package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** Single field-level validation issue for structured 400 responses. */
@Schema(description = "Bean validation violation for one field or property path")
public record FieldViolation(
    @Schema(description = "Field or property path", example = "question") String field,
    @Schema(description = "Validation message", example = "must not be blank") String message
) {}
