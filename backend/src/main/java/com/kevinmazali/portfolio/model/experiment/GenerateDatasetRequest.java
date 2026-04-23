package com.kevinmazali.portfolio.model.experiment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Start async QRA dataset generation from vector-store chunks")
public record GenerateDatasetRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    String description,
    @Schema(description = "Filter to one logical document_id from vector metadata; omit for all documents")
    String documentId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "SupportedChatModel.modelId")
    String model,
    @Schema(description = "Max successful Q&A generations per chunk (default 1)")
    Integer questionsPerChunk,
    @Schema(description = "Hard cap on total examples in the saved dataset")
    Integer maxQuestions,
    @Schema(description = "Optional RNG seed for chunk shuffle reproducibility")
    Integer seed) {}
