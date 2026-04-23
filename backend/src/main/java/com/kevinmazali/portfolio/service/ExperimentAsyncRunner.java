package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.RagAnswer;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleRow;
import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import com.kevinmazali.portfolio.util.InputValidator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs {@link ExperimentRun} work off the HTTP thread (LLM + RAG per example).
 */
@Slf4j
@Component
public class ExperimentAsyncRunner {

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentResultRepository experimentResultRepository;
  private final EvalDatasetService evalDatasetService;
  private final OpenAIService openAIService;
  private final EvaluatorService evaluatorService;

  public ExperimentAsyncRunner(
      ExperimentRunRepository experimentRunRepository,
      ExperimentResultRepository experimentResultRepository,
      EvalDatasetService evalDatasetService,
      OpenAIService openAIService,
      EvaluatorService evaluatorService) {
    this.experimentRunRepository = experimentRunRepository;
    this.experimentResultRepository = experimentResultRepository;
    this.evalDatasetService = evalDatasetService;
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
    ExperimentRun run = experimentRunRepository.findById(runId)
        .orElseThrow(() -> new IllegalStateException("Run not found: " + runId));

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

    List<Double> faith = new ArrayList<>();
    List<Double> rel = new ArrayList<>();
    List<Double> corr = new ArrayList<>();
    List<Double> conc = new ArrayList<>();

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
      String docsJoined = rag.documentTexts().stream()
          .map(s -> s == null ? "" : s)
          .collect(Collectors.joining("\n---\n"));

      EvaluationScore f = evaluatorService.evaluateFaithfulness(judge, q, answer, rag.documentTexts());
      EvaluationScore r = evaluatorService.evaluateRelevance(judge, q, answer);
      EvaluationScore c = evaluatorService.evaluateCorrectness(judge, q, answer, ex.referenceText());
      EvaluationScore co = evaluatorService.evaluateConciseness(judge, q, answer);

      if (!Double.isNaN(f.score())) {
        faith.add(f.score());
      }
      if (!Double.isNaN(r.score())) {
        rel.add(r.score());
      }
      if (!Double.isNaN(c.score())) {
        corr.add(c.score());
      }
      if (!Double.isNaN(co.score())) {
        conc.add(co.score());
      }

      ExperimentResult row = ExperimentResult.builder()
          .experimentRun(run)
          .question(q)
          .referenceAnswer(ex.referenceText() != null ? ex.referenceText() : "")
          .ragResponse(answer)
          .documents(docsJoined)
          .faithfulness(Double.isNaN(f.score()) ? null : f.score())
          .relevance(Double.isNaN(r.score()) ? null : r.score())
          .correctness(Double.isNaN(c.score()) ? null : c.score())
          .conciseness(Double.isNaN(co.score()) ? null : co.score())
          .faithfulnessExplanation(f.explanation())
          .relevanceExplanation(r.explanation())
          .correctnessExplanation(c.explanation())
          .concisenessExplanation(co.explanation())
          .build();
      experimentResultRepository.save(row);
    }

    run.setMeanFaithfulness(mean(faith));
    run.setMeanRelevance(mean(rel));
    run.setMeanCorrectness(mean(corr));
    run.setMeanConciseness(mean(conc));
    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setCompletedAt(OffsetDateTime.now());
    experimentRunRepository.save(run);
  }

  private static Double mean(List<Double> vals) {
    if (vals.isEmpty()) {
      return null;
    }
    return vals.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
