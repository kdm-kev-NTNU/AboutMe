package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Text payload for voice synthesis")
public record SynthesizeRequest(
    @Schema(description = "Plain text to synthesize", example = "Hei! Velkommen til Kevin sin portfolio.")
    String text) {}
