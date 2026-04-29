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
    @Schema(description = "Allow-listed chat model id (e.g. gpt-5.4-mini). Omit to use the server default.", example = "gpt-5.4-mini")
    String model,
    @Schema(description = "Optional client-generated conversation id used for telemetry correlation", example = "1c7a2b7e-8a96-4c5b-9b4c-9ef2c1e0d7a2")
    String conversationId
) {

  public Question(String question) {
    this(question, null);
  }

  public Question(String question, String model) {
    this(question, model, null);
  }
}
