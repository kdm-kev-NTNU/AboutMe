package com.kevinmazali.portfolio.model.experiment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Async QRA generation accepted")
public record DatasetGenerationStartResponse(long generationId, String status) {}
