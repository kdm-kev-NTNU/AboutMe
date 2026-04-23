package com.kevinmazali.portfolio.model.experiment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body to start an eval experiment run.
 */
@Schema(description = "Start experiment on an eval dataset")
public record RunExperimentRequest(
    @Schema(description = "Eval dataset id (numeric string)", requiredMode = Schema.RequiredMode.REQUIRED)
    String datasetId,
    @Schema(description = "Human-readable dataset name (for display)")
    String datasetName,
    @Schema(description = "Optional run title")
    String name,
    @Schema(description = "Generator model id (e.g. gpt-5.4-mini)", requiredMode = Schema.RequiredMode.REQUIRED)
    String generatorModel,
    @Schema(description = "Evaluator / judge model id", requiredMode = Schema.RequiredMode.REQUIRED)
    String evaluatorModel,
    @Schema(description = "Optional cap on number of examples")
    Integer maxExamples
) {
}
