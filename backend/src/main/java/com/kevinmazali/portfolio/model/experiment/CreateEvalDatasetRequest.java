package com.kevinmazali.portfolio.model.experiment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Create an eval dataset from examples")
public record CreateEvalDatasetRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<DatasetExampleInput> examples) {
  public record DatasetExampleInput(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
      String question,
      String referenceText) {}
}
