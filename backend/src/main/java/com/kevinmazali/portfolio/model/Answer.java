package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing an answer returned by the AI service.
 */
@Schema(description = "AI-generated answer text")
public record Answer(
    @Schema(description = "Markdown-capable answer body")
    String answer
) {
}
