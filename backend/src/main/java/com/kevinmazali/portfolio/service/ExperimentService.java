package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.CreatePhoenixDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentResultResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExperimentService {

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentResultRepository experimentResultRepository;
  private final PhoenixDatasetService phoenixDatasetService;
  private final PhoenixProperties phoenixProperties;
  private final ChatModelCatalog chatModelCatalog;
  private final ExperimentAsyncRunner experimentAsyncRunner;

  public ExperimentService(
      ExperimentRunRepository experimentRunRepository,
      ExperimentResultRepository experimentResultRepository,
      PhoenixDatasetService phoenixDatasetService,
      PhoenixProperties phoenixProperties,
      ChatModelCatalog chatModelCatalog,
      ExperimentAsyncRunner experimentAsyncRunner) {
    this.experimentRunRepository = experimentRunRepository;
    this.experimentResultRepository = experimentResultRepository;
    this.phoenixDatasetService = phoenixDatasetService;
    this.phoenixProperties = phoenixProperties;
    this.chatModelCatalog = chatModelCatalog;
    this.experimentAsyncRunner = experimentAsyncRunner;
  }

  public List<PhoenixDatasetSummary> listPhoenixDatasets() {
    return phoenixDatasetService.listDatasets();
  }

  public void deletePhoenixDataset(String datasetId) {
    phoenixDatasetService.deleteDataset(datasetId);
  }

  public PhoenixDatasetSummary createPhoenixDataset(CreatePhoenixDatasetRequest request) {
    if (request.examples() == null || request.examples().isEmpty()) {
      throw new IllegalArgumentException("examples required");
    }
    List<Map<String, Object>> inputs = new ArrayList<>();
    List<Map<String, Object>> outputs = new ArrayList<>();
    for (CreatePhoenixDatasetRequest.DatasetExampleInput ex : request.examples()) {
      if (!StringUtils.hasText(ex.question())) {
        throw new IllegalArgumentException("Each example needs a question");
      }
      Map<String, Object> in = new HashMap<>();
      in.put("question", ex.question().trim());
      inputs.add(in);
      Map<String, Object> out = new HashMap<>();
      out.put("reference_text", ex.referenceText() != null ? ex.referenceText() : "");
      outputs.add(out);
    }
    return phoenixDatasetService.createDataset(
        request.name(),
        request.description() != null ? request.description() : "",
        inputs,
        outputs);
  }

  public long startRun(RunExperimentRequest request) {
    if (!phoenixDatasetService.isEnabled()) {
      throw new IllegalStateException("Phoenix REST is not configured (portfolio.phoenix.base-url).");
    }
    if (!StringUtils.hasText(request.datasetId())) {
      throw new IllegalArgumentException("datasetId is required");
    }
    SupportedChatModel gen = SupportedChatModel.fromModelId(request.generatorModel())
        .orElseThrow(() -> new IllegalArgumentException("Unknown generator model"));
    SupportedChatModel ev = SupportedChatModel.fromModelId(request.evaluatorModel())
        .orElseThrow(() -> new IllegalArgumentException("Unknown evaluator model"));
    if (!chatModelCatalog.isModelConfigured(gen)) {
      throw new IllegalArgumentException("Generator model is not configured (API key).");
    }
    if (!chatModelCatalog.isModelConfigured(ev)) {
      throw new IllegalArgumentException("Evaluator model is not configured (API key).");
    }
    if (gen.provider() == ev.provider()) {
      throw new IllegalArgumentException(
          "Generator and evaluator must be from different providers to avoid model-family bias.");
    }

    List<PhoenixDatasetExample> examples = phoenixDatasetService.getExamples(request.datasetId().trim());
    if (examples.isEmpty()) {
      throw new IllegalArgumentException("Dataset has no examples.");
    }
    int n = examples.size();
    if (request.maxExamples() != null && request.maxExamples() > 0) {
      n = Math.min(n, request.maxExamples());
    }

    String dsName = StringUtils.hasText(request.datasetName())
        ? request.datasetName().trim()
        : request.datasetId();
    String runName = StringUtils.hasText(request.name())
        ? request.name().trim()
        : "Experiment " + OffsetDateTime.now();

    ExperimentRun run = ExperimentRun.builder()
        .name(runName)
        .datasetName(dsName)
        .phoenixDatasetId(request.datasetId().trim())
        .phoenixBaseUrl(phoenixProperties.getBaseUrl() != null ? phoenixProperties.getBaseUrl().trim() : "")
        .generatorModel(gen.modelId())
        .evaluatorModel(ev.modelId())
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(n)
        .build();
    run = experimentRunRepository.save(run);
    experimentAsyncRunner.executeExperimentRun(run.getId());
    return run.getId();
  }

  public List<ExperimentRunSummaryResponse> listRuns() {
    return experimentRunRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toSummary)
        .toList();
  }

  public Optional<ExperimentRunDetailResponse> getRun(long id) {
    return experimentRunRepository.findById(id).map(this::toDetail);
  }

  public Optional<ExperimentRunSummaryResponse> getStatus(long id) {
    return experimentRunRepository.findById(id).map(this::toSummary);
  }

  private ExperimentRunSummaryResponse toSummary(ExperimentRun r) {
    return new ExperimentRunSummaryResponse(
        r.getId(),
        r.getName(),
        r.getDatasetName(),
        r.getGeneratorModel(),
        r.getEvaluatorModel(),
        r.getStatus(),
        r.getTotalExamples() != null ? r.getTotalExamples() : 0,
        r.getMeanFaithfulness(),
        r.getMeanRelevance(),
        r.getMeanCorrectness(),
        r.getMeanConciseness(),
        r.getErrorMessage(),
        r.getCreatedAt(),
        r.getCompletedAt());
  }

  private ExperimentRunDetailResponse toDetail(ExperimentRun r) {
    List<ExperimentResult> rows = experimentResultRepository.findByExperimentRunIdOrderByIdAsc(r.getId());
    List<ExperimentResultResponse> res = rows.stream().map(this::toResultResponse).toList();
    return new ExperimentRunDetailResponse(
        r.getId(),
        r.getName(),
        r.getDatasetName(),
        r.getPhoenixDatasetId(),
        r.getPhoenixBaseUrl(),
        r.getGeneratorModel(),
        r.getEvaluatorModel(),
        r.getStatus(),
        r.getTotalExamples() != null ? r.getTotalExamples() : 0,
        r.getMeanFaithfulness(),
        r.getMeanRelevance(),
        r.getMeanCorrectness(),
        r.getMeanConciseness(),
        r.getErrorMessage(),
        r.getCreatedAt(),
        r.getCompletedAt(),
        res);
  }

  private ExperimentResultResponse toResultResponse(ExperimentResult row) {
    String preview = row.getDocuments();
    if (preview != null && preview.length() > 500) {
      preview = preview.substring(0, 500) + "...";
    }
    return new ExperimentResultResponse(
        row.getId(),
        row.getQuestion(),
        row.getReferenceAnswer(),
        row.getRagResponse(),
        preview,
        row.getFaithfulness(),
        row.getRelevance(),
        row.getCorrectness(),
        row.getConciseness(),
        row.getFaithfulnessExplanation(),
        row.getRelevanceExplanation(),
        row.getCorrectnessExplanation(),
        row.getConcisenessExplanation());
  }
}
