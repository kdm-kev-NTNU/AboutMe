package com.kevinmazali.portfolio.model.experiment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pollable status for async QRA dataset generation")
public record DatasetGenerationStatusResponse(
    long id,
    String status,
    Integer questionsGenerated,
    String resultDatasetId,
    String errorMessage,
    String createdAt,
    String completedAt) {}
