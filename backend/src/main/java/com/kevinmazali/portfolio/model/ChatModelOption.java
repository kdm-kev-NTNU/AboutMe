package com.kevinmazali.portfolio.model;

import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.ModelTag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Selectable chat model for the RAG UI")
public record ChatModelOption(
    @Schema(description = "Provider API model id", example = "gpt-5.4-mini")
    String id,
    @Schema(description = "LLM vendor")
    ChatProvider provider,
    @Schema(description = "Human-readable label")
    String label,
    @Schema(description = "Capability tags (e.g. FAST vs REASONING)")
    Set<ModelTag> tags
) {
}
