package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleRow;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.ExperimentMetricScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.model.experiment.ExperimentResultResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.repository.EvalDatasetRepository;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExperimentService {

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentResultRepository experimentResultRepository;
  private final EvalDatasetRepository evalDatasetRepository;
  private final EvalDatasetService evalDatasetService;
  private final ExperimentMetricsService experimentMetricsService;
  private final PostHogProperties postHogProperties;
  private final ChatModelCatalog chatModelCatalog;
  private final ExperimentAsyncRunner experimentAsyncRunner;

  public ExperimentService(
      ExperimentRunRepository experimentRunRepository,
      ExperimentResultRepository experimentResultRepository,
      EvalDatasetRepository evalDatasetRepository,
      EvalDatasetService evalDatasetService,
      ExperimentMetricsService experimentMetricsService,
      PostHogProperties postHogProperties,
      ChatModelCatalog chatModelCatalog,
      ExperimentAsyncRunner experimentAsyncRunner) {
    this.experimentRunRepository = experimentRunRepository;
    this.experimentResultRepository = experimentResultRepository;
    this.evalDatasetRepository = evalDatasetRepository;
    this.evalDatasetService = evalDatasetService;
    this.experimentMetricsService = experimentMetricsService;
    this.postHogProperties = postHogProperties;
    this.chatModelCatalog = chatModelCatalog;
    this.experimentAsyncRunner = experimentAsyncRunner;
  }

  public List<EvalDatasetSummary> listEvalDatasets() {
    return evalDatasetService.listDatasets();
  }

  public void deleteEvalDataset(String datasetId) {
    evalDatasetService.deleteDataset(datasetId);
  }

  public EvalDatasetSummary createEvalDataset(CreateEvalDatasetRequest request) {
    return evalDatasetService.createDataset(request);
  }

  public long startRun(RunExperimentRequest request) {
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

    long evalDatasetId = parseDatasetId(request.datasetId().trim());
    EvalDatasetEntity evalDataset =
        evalDatasetRepository
            .findById(evalDatasetId)
            .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + evalDatasetId));
    List<EvalDatasetExampleRow> examples = evalDatasetService.getExamples(Long.toString(evalDatasetId));
    if (examples.isEmpty()) {
      throw new IllegalArgumentException("Dataset has no examples.");
    }
    int n = examples.size();
    if (request.maxExamples() != null && request.maxExamples() > 0) {
      n = Math.min(n, request.maxExamples());
    }

    String runName = StringUtils.hasText(request.name())
        ? request.name().trim()
        : "Experiment " + OffsetDateTime.now();

    ExperimentRun run =
        ExperimentRun.builder()
            .name(runName)
            .evalDataset(evalDataset)
            .generatorModel(gen.modelId())
            .evaluatorModel(ev.modelId())
            .status(ExperimentRunStatus.RUNNING)
            .totalExamples(n)
            .build();
    run = experimentRunRepository.save(run);
    experimentAsyncRunner.executeExperimentRun(run.getId());
    return run.getId();
  }

  private static long parseDatasetId(String datasetId) {
    try {
      return Long.parseLong(datasetId.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid dataset id: " + datasetId);
    }
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
    Map<String, Double> means = experimentMetricsService.meanScoresForRun(r.getId());
    return new ExperimentRunSummaryResponse(
        r.getId(),
        r.getName(),
        datasetName(r),
        r.getGeneratorModel(),
        r.getEvaluatorModel(),
        r.getStatus(),
        r.getTotalExamples() != null ? r.getTotalExamples() : 0,
        means.get("faithfulness"),
        means.get("relevance"),
        means.get("correctness"),
        means.get("conciseness"),
        means.get("language_consistency"),
        r.getErrorMessage(),
        r.getCreatedAt(),
        r.getCompletedAt());
  }

  private ExperimentRunDetailResponse toDetail(ExperimentRun r) {
    List<ExperimentResult> rows = experimentResultRepository.findByExperimentRunIdOrderByIdAsc(r.getId());
    List<ExperimentResultResponse> res = rows.stream().map(this::toResultResponse).toList();
    Map<String, Double> means = experimentMetricsService.meanScoresForRun(r.getId());
    String ph = postHogProperties.getHost() != null ? postHogProperties.getHost().trim() : "";
    return new ExperimentRunDetailResponse(
        r.getId(),
        r.getName(),
        datasetName(r),
        r.getEvalDatasetId(),
        ph,
        r.getGeneratorModel(),
        r.getEvaluatorModel(),
        r.getStatus(),
        r.getTotalExamples() != null ? r.getTotalExamples() : 0,
        means.get("faithfulness"),
        means.get("relevance"),
        means.get("correctness"),
        means.get("conciseness"),
        means.get("language_consistency"),
        r.getErrorMessage(),
        r.getCreatedAt(),
        r.getCompletedAt(),
        res);
  }

  private String datasetName(ExperimentRun r) {
    if (r.getEvalDataset() != null && r.getEvalDataset().getName() != null) {
      return r.getEvalDataset().getName();
    }
    if (r.getEvalDatasetId() != null) {
      return evalDatasetRepository.findById(r.getEvalDatasetId()).map(EvalDatasetEntity::getName).orElse("");
    }
    return "";
  }

  private ExperimentResultResponse toResultResponse(ExperimentResult row) {
    String preview = row.getRetrievedContext();
    if (preview != null && preview.length() > 500) {
      preview = preview.substring(0, 500) + "...";
    }
    Map<String, ExperimentMetricScore> byMetric =
        experimentMetricsService.scoresForResult(row.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ExperimentMetricScore::getMetric, s -> s, (a, b) -> a));
    return new ExperimentResultResponse(
        row.getId(),
        row.getQuestion(),
        row.getReferenceAnswer(),
        row.getRagResponse(),
        preview,
        score(byMetric, "faithfulness"),
        score(byMetric, "relevance"),
        score(byMetric, "correctness"),
        score(byMetric, "conciseness"),
        score(byMetric, "language_consistency"),
        explanation(byMetric, "faithfulness"),
        explanation(byMetric, "relevance"),
        explanation(byMetric, "correctness"),
        explanation(byMetric, "conciseness"),
        explanation(byMetric, "language_consistency"));
  }

  private static Double score(Map<String, ExperimentMetricScore> byMetric, String metric) {
    ExperimentMetricScore s = byMetric.get(metric);
    return s != null ? s.getScore() : null;
  }

  private static String explanation(Map<String, ExperimentMetricScore> byMetric, String metric) {
    ExperimentMetricScore s = byMetric.get(metric);
    return s != null ? s.getExplanation() : null;
  }
}
