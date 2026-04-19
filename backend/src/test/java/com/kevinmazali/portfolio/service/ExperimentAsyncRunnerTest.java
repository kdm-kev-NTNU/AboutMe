package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.RagAnswer;
import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ExperimentAsyncRunner#executeExperimentRun} is {@code @Async} on the Spring proxy only;
 * calling the runner directly runs synchronously and is suitable for unit tests.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentAsyncRunnerTest {

  @Mock
  private ExperimentRunRepository experimentRunRepository;

  @Mock
  private ExperimentResultRepository experimentResultRepository;

  @Mock
  private PhoenixDatasetService phoenixDatasetService;

  @Mock
  private OpenAIService openAIService;

  @Mock
  private EvaluatorService evaluatorService;

  private ExperimentAsyncRunner runner;

  @BeforeEach
  void setUp() {
    runner = new ExperimentAsyncRunner(
        experimentRunRepository,
        experimentResultRepository,
        phoenixDatasetService,
        openAIService,
        evaluatorService);
  }

  @Test
  void runNotFoundDoesNotPersistFailureWhenRunMissingInErrorHandler() {
    when(experimentRunRepository.findById(9L)).thenReturn(Optional.empty());

    runner.executeExperimentRun(9L);

    verify(experimentRunRepository, times(2)).findById(9L);
    verify(experimentRunRepository, never()).save(any());
    verify(experimentResultRepository, never()).save(any());
  }

  @Test
  void invalidDatasetQuestionMarksRunFailed() {
    ExperimentRun run = baseRun();
    when(experimentRunRepository.findById(1L)).thenReturn(Optional.of(run));
    when(phoenixDatasetService.getExamples("phx-1")).thenReturn(List.of(
        new PhoenixDatasetExample("<script>x</script>", "ref", Map.of(), Map.of())));

    runner.executeExperimentRun(1L);

    ArgumentCaptor<ExperimentRun> cap = ArgumentCaptor.forClass(ExperimentRun.class);
    verify(experimentRunRepository, times(2)).findById(1L);
    verify(experimentRunRepository).save(cap.capture());
    assertEquals(ExperimentRunStatus.FAILED, cap.getValue().getStatus());
    assertNotNull(cap.getValue().getErrorMessage());
    verify(experimentResultRepository, never()).save(any(ExperimentResult.class));
  }

  @Test
  void completesRunAndPersistsOneResultPerExample() {
    ExperimentRun run = baseRun();
    run.setTotalExamples(1);
    when(experimentRunRepository.findById(1L)).thenReturn(Optional.of(run));
    when(phoenixDatasetService.getExamples("phx-1")).thenReturn(List.of(
        new PhoenixDatasetExample("What is two plus two?", "four", Map.of(), Map.of()),
        new PhoenixDatasetExample("Other?", "x", Map.of(), Map.of())));

    when(openAIService.getAnswerWithDocuments(any(Question.class)))
        .thenReturn(new RagAnswer("4", List.of("chunk-a")));
    when(evaluatorService.evaluateFaithfulness(any(), any(), any(), any()))
        .thenReturn(new EvaluationScore(0.8, "ok", "f"));
    when(evaluatorService.evaluateRelevance(any(), any(), any()))
        .thenReturn(new EvaluationScore(0.7, "ok", "r"));
    when(evaluatorService.evaluateCorrectness(any(), any(), any(), any()))
        .thenReturn(new EvaluationScore(0.9, "ok", "c"));
    when(evaluatorService.evaluateConciseness(any(), any(), any()))
        .thenReturn(EvaluationScore.failed("skip"));

    runner.executeExperimentRun(1L);

    verify(experimentResultRepository).save(any(ExperimentResult.class));
    ArgumentCaptor<ExperimentRun> runCap = ArgumentCaptor.forClass(ExperimentRun.class);
    verify(experimentRunRepository).save(runCap.capture());
    ExperimentRun saved = runCap.getValue();
    assertEquals(ExperimentRunStatus.COMPLETED, saved.getStatus());
    assertNotNull(saved.getCompletedAt());
    assertEquals(0.8, saved.getMeanFaithfulness(), 0.001);
    assertEquals(0.7, saved.getMeanRelevance(), 0.001);
    assertEquals(0.9, saved.getMeanCorrectness(), 0.001);
    assertNull(saved.getMeanConciseness());
  }

  private static ExperimentRun baseRun() {
    return ExperimentRun.builder()
        .id(1L)
        .name("n")
        .datasetName("d")
        .phoenixDatasetId("phx-1")
        .generatorModel("gpt-test")
        .evaluatorModel("gpt-judge")
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(0)
        .build();
  }
}
