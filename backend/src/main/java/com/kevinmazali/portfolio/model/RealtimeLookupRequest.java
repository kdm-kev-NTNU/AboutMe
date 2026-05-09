package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lookup request for the Realtime voice assistant knowledge tool")
public record RealtimeLookupRequest(
    @Schema(description = "User question or lookup query", example = "What does Kevin study?")
    String query,
    @Schema(description = "Preferred response language: en or no", example = "en")
    String language) {}
