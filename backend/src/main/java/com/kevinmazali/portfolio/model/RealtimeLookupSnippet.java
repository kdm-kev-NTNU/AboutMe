package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A short public knowledge snippet for the Realtime voice assistant")
public record RealtimeLookupSnippet(
    @Schema(description = "Snippet source: profile or rag", example = "profile")
    String sourceType,
    @Schema(description = "Short source title", example = "Data engineering at NTNU")
    String title,
    @Schema(description = "Concise public fact text")
    String text) {}
