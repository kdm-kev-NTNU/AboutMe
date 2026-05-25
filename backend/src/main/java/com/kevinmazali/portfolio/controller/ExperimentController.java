package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStartResponse;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatusResponse;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.GenerateDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.service.DatasetGenerationService;
import com.kevinmazali.portfolio.service.ExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tools/experiments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Experiments", description = "Eval datasets and RAG eval runs")
public class ExperimentController {

  private final ExperimentService experimentService;
  private final DatasetGenerationService datasetGenerationService;
  private final ChatModelCatalog chatModelCatalog;
  private final PostHogProperties postHogProperties;

  @Operation(summary = "PostHog + eval config for admin UI links")
  @GetMapping("/config")
  public Map<String, Object> analyticsConfig() {
    String host = postHogProperties.getHost() != null ? postHogProperties.getHost().trim() : "";
    return Map.of(
        "posthogConfigured", postHogProperties.isCaptureConfigured(),
        "posthogHost", host);
  }

  @Operation(summary = "List eval datasets")
  @GetMapping("/datasets")
  public List<EvalDatasetSummary> listDatasets() {
    return experimentService.listEvalDatasets();
  }

  @Operation(summary = "Delete eval dataset by id")
  @DeleteMapping("/datasets/{id}")
  public ResponseEntity<?> deleteDataset(@PathVariable("id") String id) {
    try {
      experimentService.deleteEvalDataset(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiError("Delete failed: " + e.getMessage()));
    }
  }

  @Operation(summary = "Generate eval dataset from vector chunks (async QRA)")
  @PostMapping("/datasets/generate")
  public ResponseEntity<?> generateDataset(@RequestBody GenerateDatasetRequest body) {
    try {
      long id = datasetGenerationService.startGeneration(body);
      return ResponseEntity.accepted()
          .body(new DatasetGenerationStartResponse(id, DatasetGenerationStatus.RUNNING));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ApiError(e.getMessage()));
    }
  }

  @Operation(summary = "Poll async dataset generation status")
  @GetMapping("/datasets/generate/{id}/status")
  public ResponseEntity<?> generationStatus(@PathVariable long id) {
    return datasetGenerationService
        .getGenerationStatus(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Create eval dataset")
  @PostMapping("/datasets")
  public ResponseEntity<?> createDataset(@RequestBody CreateEvalDatasetRequest body) {
    try {
      EvalDatasetSummary created = experimentService.createEvalDataset(body);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ApiError(e.getMessage()));
    }
  }

  @Operation(summary = "Models available for generator and evaluator")
  @GetMapping("/models")
  public List<ChatModelOption> models() {
    return chatModelCatalog.listAvailableModels();
  }

  @Operation(summary = "Start experiment run (async)")
  @PostMapping("/run")
  public ResponseEntity<?> startRun(@RequestBody RunExperimentRequest body) {
    try {
      long id = experimentService.startRun(body);
      return ResponseEntity.accepted().body(Map.of("runId", id));
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ApiError(e.getMessage()));
    }
  }

  @Operation(summary = "List experiment runs")
  @GetMapping("/runs")
  public List<ExperimentRunSummaryResponse> listRuns() {
    return experimentService.listRuns();
  }

  @Operation(summary = "Experiment run detail with per-example scores")
  @GetMapping("/runs/{id}")
  public ResponseEntity<?> getRun(@PathVariable long id) {
    return experimentService.getRun(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Experiment run status summary")
  @GetMapping("/runs/{id}/status")
  public ResponseEntity<?> status(@PathVariable long id) {
    return experimentService.getStatus(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
