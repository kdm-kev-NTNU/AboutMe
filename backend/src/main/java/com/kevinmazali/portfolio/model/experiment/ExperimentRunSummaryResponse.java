package com.kevinmazali.portfolio.model.experiment;

import java.time.OffsetDateTime;

public record ExperimentRunSummaryResponse(
    long id,
    String name,
    String datasetName,
    String generatorModel,
    String evaluatorModel,
    ExperimentRunStatus status,
    int totalExamples,
    Double meanFaithfulness,
    Double meanRelevance,
    Double meanCorrectness,
    Double meanConciseness,
    Double meanLanguageConsistency,
    String errorMessage,
    OffsetDateTime createdAt,
    OffsetDateTime completedAt
) {
}
