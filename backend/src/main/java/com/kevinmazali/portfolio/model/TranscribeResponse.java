package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** JSON body for {@code POST /transcribe}. */
@Schema(description = "Speech-to-text transcript result")
public record TranscribeResponse(
    @Schema(description = "Recognized text from the uploaded audio", requiredMode = Schema.RequiredMode.REQUIRED)
    String text) {
}
