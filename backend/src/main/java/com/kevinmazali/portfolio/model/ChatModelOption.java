package com.kevinmazali.portfolio.model;

import com.kevinmazali.portfolio.model.chat.ChatProvider;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Selectable chat model for the RAG UI")
public record ChatModelOption(
    @Schema(description = "Provider API model id", example = "gpt-4o-mini")
    String id,
    @Schema(description = "LLM vendor")
    ChatProvider provider,
    @Schema(description = "Human-readable label")
    String label
) {
}
