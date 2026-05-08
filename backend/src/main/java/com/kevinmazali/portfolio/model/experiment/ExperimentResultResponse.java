package com.kevinmazali.portfolio.model.experiment;

public record ExperimentResultResponse(
    long id,
    String question,
    String referenceAnswer,
    String ragResponse,
    String documentsPreview,
    Double faithfulness,
    Double relevance,
    Double correctness,
    Double conciseness,
    Double languageConsistency,
    String faithfulnessExplanation,
    String relevanceExplanation,
    String correctnessExplanation,
    String concisenessExplanation,
    String languageConsistencyExplanation
) {
}
