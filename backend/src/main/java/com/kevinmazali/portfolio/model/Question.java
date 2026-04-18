package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO carrying a user question submitted to the API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User question for the RAG chat endpoint")
public record Question(
    @Schema(description = "Natural language question", example = "What projects have you worked on?", maxLength = 3000)
    String question,
    @Schema(description = "Allow-listed chat model id (e.g. gpt-4o-mini). Omit to use the server default.", example = "gpt-4o-mini")
    String model
) {

  public Question(String question) {
    this(question, null);
  }
}
