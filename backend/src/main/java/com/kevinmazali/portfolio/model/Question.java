package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO carrying a user question submitted to the API.
 */
@Schema(description = "User question for the RAG chat endpoint")
public record Question(
    @Schema(description = "Natural language question", example = "What projects have you worked on?", maxLength = 3000)
    String question
) {
}
