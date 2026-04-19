package com.kevinmazali.portfolio.model.experiment;

import java.time.OffsetDateTime;
import java.util.List;

public record ExperimentRunDetailResponse(
    long id,
    String name,
    String datasetName,
    String phoenixDatasetId,
    String phoenixBaseUrl,
    String generatorModel,
    String evaluatorModel,
    ExperimentRunStatus status,
    int totalExamples,
    Double meanFaithfulness,
    Double meanRelevance,
    Double meanCorrectness,
    Double meanConciseness,
    String errorMessage,
    OffsetDateTime createdAt,
    OffsetDateTime completedAt,
    List<ExperimentResultResponse> results
) {
}
