package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.experiment.CreatePhoenixDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.service.ExperimentService;
import com.kevinmazali.portfolio.service.PhoenixDatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tools/experiments")
@RequiredArgsConstructor
@Tag(name = "Experiments", description = "Phoenix datasets and RAG eval runs")
public class ExperimentController {

  private final ExperimentService experimentService;
  private final PhoenixDatasetService phoenixDatasetService;
  private final ChatModelCatalog chatModelCatalog;
  private final PhoenixProperties phoenixProperties;

  @Operation(summary = "Phoenix REST config for UI links")
  @GetMapping("/config")
  public Map<String, Object> phoenixConfig() {
    String base = phoenixProperties.getBaseUrl() != null ? phoenixProperties.getBaseUrl().trim() : "";
    return Map.of(
        "phoenixConfigured", phoenixDatasetService.isEnabled(),
        "phoenixBaseUrl", base);
  }

  @Operation(summary = "List Phoenix datasets")
  @GetMapping("/datasets")
  public ResponseEntity<?> listDatasets() {
    if (!phoenixDatasetService.isEnabled()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new ApiError("Phoenix REST is not configured. Set PHOENIX_BASE_URL / portfolio.phoenix.base-url."));
    }
    try {
      List<PhoenixDatasetSummary> list = experimentService.listPhoenixDatasets();
      return ResponseEntity.ok(list);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(new ApiError("Phoenix list failed: " + e.getMessage()));
    }
  }

  @Operation(summary = "Delete Phoenix dataset by id")
  @DeleteMapping("/datasets/{id}")
  public ResponseEntity<?> deleteDataset(@PathVariable("id") String id) {
    if (!phoenixDatasetService.isEnabled()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new ApiError("Phoenix REST is not configured."));
    }
    try {
      experimentService.deletePhoenixDataset(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(new ApiError("Phoenix delete failed: " + e.getMessage()));
    }
  }

  @Operation(summary = "Create Phoenix dataset")
  @PostMapping("/datasets")
  public ResponseEntity<?> createDataset(@RequestBody CreatePhoenixDatasetRequest body) {
    if (!phoenixDatasetService.isEnabled()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new ApiError("Phoenix REST is not configured."));
    }
    try {
      PhoenixDatasetSummary created = experimentService.createPhoenixDataset(body);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(new ApiError("Phoenix create failed: " + e.getMessage()));
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
