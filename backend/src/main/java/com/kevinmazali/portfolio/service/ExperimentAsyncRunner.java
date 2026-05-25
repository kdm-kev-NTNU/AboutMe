package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.RagAnswer;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleRow;
import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.repository.EvalDatasetExampleRepository;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import com.kevinmazali.portfolio.util.InputValidator;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExperimentAsyncRunner {

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentResultRepository experimentResultRepository;
  private final EvalDatasetExampleRepository evalDatasetExampleRepository;
  private final EvalDatasetService evalDatasetService;
  private final ExperimentMetricsService experimentMetricsService;
  private final OpenAIService openAIService;
  private final EvaluatorService evaluatorService;

  public ExperimentAsyncRunner(
      ExperimentRunRepository experimentRunRepository,
      ExperimentResultRepository experimentResultRepository,
      EvalDatasetExampleRepository evalDatasetExampleRepository,
      EvalDatasetService evalDatasetService,
      ExperimentMetricsService experimentMetricsService,
      OpenAIService openAIService,
      EvaluatorService evaluatorService) {
    this.experimentRunRepository = experimentRunRepository;
    this.experimentResultRepository = experimentResultRepository;
    this.evalDatasetExampleRepository = evalDatasetExampleRepository;
    this.evalDatasetService = evalDatasetService;
    this.experimentMetricsService = experimentMetricsService;
    this.openAIService = openAIService;
    this.evaluatorService = evaluatorService;
  }

  @Async("experimentTaskExecutor")
  public void executeExperimentRun(Long runId) {
    try {
      runInternal(runId);
    } catch (Exception e) {
      log.error("Experiment run {} failed", runId, e);
      experimentRunRepository.findById(runId).ifPresent(run -> {
        run.setStatus(ExperimentRunStatus.FAILED);
        run.setErrorMessage(truncate(e.getMessage(), 4000));
        run.setCompletedAt(OffsetDateTime.now());
        experimentRunRepository.save(run);
      });
    }
  }

  private void runInternal(Long runId) {
    ExperimentRun run =
        experimentRunRepository.findById(runId).orElseThrow(() -> new IllegalStateException("Run not found: " + runId));

    if (run.getEvalDatasetId() == null) {
      throw new IllegalStateException("Run has no eval dataset id");
    }
    List<EvalDatasetExampleRow> examples =
        evalDatasetService.getExamples(Long.toString(run.getEvalDatasetId()));
    int limit = run.getTotalExamples() != null ? run.getTotalExamples() : examples.size();
    if (limit <= 0) {
      limit = examples.size();
    }
    List<EvalDatasetExampleRow> slice = examples.subList(0, Math.min(limit, examples.size()));

    String gen = run.getGeneratorModel();
    String judge = run.getEvaluatorModel();

    for (EvalDatasetExampleRow ex : slice) {
      String qRaw = ex.question() == null ? "" : ex.question();
      String q = InputValidator.sanitizeString(qRaw);
      if (!InputValidator.isValidQuestion(q)) {
        throw new IllegalArgumentException("Invalid dataset question (validation failed): " + qRaw);
      }

      RagAnswer rag = openAIService.getAnswerWithDocuments(new Question(q, gen));
      String answer = rag.answer() != null ? rag.answer() : "";
      String docsJoined =
          rag.documentTexts().stream().map(s -> s == null ? "" : s).collect(Collectors.joining("\n---\n"));

      EvaluationScore f = evaluatorService.evaluateFaithfulness(judge, q, answer, rag.documentTexts());
      EvaluationScore r = evaluatorService.evaluateRelevance(judge, q, answer);
      EvaluationScore c = evaluatorService.evaluateCorrectness(judge, q, answer, ex.referenceText());
      EvaluationScore co = evaluatorService.evaluateConciseness(judge, q, answer);
      EvaluationScore lc = evaluatorService.evaluateLanguageConsistency(judge, q, answer);

      ExperimentResult row =
          ExperimentResult.builder()
              .experimentRun(run)
              .evalExample(resolveExample(ex.id()))
              .question(q)
              .referenceAnswer(ex.referenceText() != null ? ex.referenceText() : "")
              .ragResponse(answer)
              .retrievedContext(docsJoined)
              .build();
      row = experimentResultRepository.save(row);

      Map<String, EvaluationScore> scores = new LinkedHashMap<>();
      scores.put("faithfulness", f);
      scores.put("relevance", r);
      scores.put("correctness", c);
      scores.put("conciseness", co);
      scores.put("language_consistency", lc);
      experimentMetricsService.saveScores(row, scores);
    }

    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setCompletedAt(OffsetDateTime.now());
    experimentRunRepository.save(run);
  }

  private EvalDatasetExampleEntity resolveExample(Long exampleId) {
    if (exampleId == null) {
      return null;
    }
    return evalDatasetExampleRepository.findById(exampleId).orElse(null);
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
