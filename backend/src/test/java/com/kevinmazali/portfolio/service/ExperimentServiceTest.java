package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.CreatePhoenixDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

  @Mock
  private ExperimentRunRepository experimentRunRepository;

  @Mock
  private ExperimentResultRepository experimentResultRepository;

  @Mock
  private PhoenixDatasetService phoenixDatasetService;

  @Mock
  private PhoenixProperties phoenixProperties;

  @Mock
  private ChatModelCatalog chatModelCatalog;

  @Mock
  private ExperimentAsyncRunner experimentAsyncRunner;

  private ExperimentService experimentService;

  @BeforeEach
  void setUp() {
    experimentService = new ExperimentService(
        experimentRunRepository,
        experimentResultRepository,
        phoenixDatasetService,
        phoenixProperties,
        chatModelCatalog,
        experimentAsyncRunner);
  }

  @Test
  void createPhoenixDatasetRejectsEmptyExamples() {
    var req = new CreatePhoenixDatasetRequest("ds", "d", List.of());
    assertThrows(IllegalArgumentException.class, () -> experimentService.createPhoenixDataset(req));
  }

  @Test
  void createPhoenixDatasetRejectsBlankQuestion() {
    var req = new CreatePhoenixDatasetRequest(
        "ds",
        null,
        List.of(new CreatePhoenixDatasetRequest.DatasetExampleInput("  ", null)));
    assertThrows(IllegalArgumentException.class, () -> experimentService.createPhoenixDataset(req));
  }

  @Test
  void startRunRequiresPhoenixEnabled() {
    when(phoenixDatasetService.isEnabled()).thenReturn(false);
    var req = new RunExperimentRequest("id", "n", null, "gpt-5.4-mini", "gpt-5.4-mini", null);
    assertThrows(IllegalStateException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunRequiresDatasetId() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    var req = new RunExperimentRequest("  ", "n", null, "gpt-5.4-mini", "gpt-5.4-mini", null);
    assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunRejectsEmptyDatasetExamples() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(phoenixDatasetService.getExamples("ds-1")).thenReturn(List.of());
    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "claude-haiku-4-5-20251001", null);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.CLAUDE_HAIKU_4_5)).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunPersistsRunAndTriggersAsync() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(phoenixDatasetService.getExamples("ds-1")).thenReturn(
        List.of(new PhoenixDatasetExample("What?", "Ref", null, null)));
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.CLAUDE_HAIKU_4_5)).thenReturn(true);
    when(phoenixProperties.getBaseUrl()).thenReturn("http://phoenix:6006");

    ExperimentRun saved = ExperimentRun.builder()
        .id(42L)
        .name("Experiment test")
        .datasetName("n")
        .phoenixDatasetId("ds-1")
        .phoenixBaseUrl("http://phoenix:6006")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("claude-haiku-4-5-20251001")
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(1)
        .build();
    when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(saved);

    long id = experimentService.startRun(
        new RunExperimentRequest(
            "ds-1", "n", "Experiment test", "gpt-5.4-mini", "claude-haiku-4-5-20251001", null));

    assertEquals(42L, id);
    ArgumentCaptor<ExperimentRun> captor = ArgumentCaptor.forClass(ExperimentRun.class);
    verify(experimentRunRepository).save(captor.capture());
    assertEquals(ExperimentRunStatus.RUNNING, captor.getValue().getStatus());
    assertEquals(1, captor.getValue().getTotalExamples());
    verify(experimentAsyncRunner).executeExperimentRun(42L);
  }

  @Test
  void getRunReturnsEmptyWhenMissing() {
    when(experimentRunRepository.findById(9L)).thenReturn(Optional.empty());
    assertTrue(experimentService.getRun(9L).isEmpty());
  }

  @Test
  void listRunsDelegatesToRepository() {
    when(experimentRunRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
    assertTrue(experimentService.listRuns().isEmpty());
    verify(experimentResultRepository, never()).findByExperimentRunIdOrderByIdAsc(any());
  }

  @Test
  void createPhoenixDatasetBuildsInputsAndDelegates() {
    var req = new CreatePhoenixDatasetRequest(
        "ds",
        "desc",
        List.of(
            new CreatePhoenixDatasetRequest.DatasetExampleInput(" Q1 ", "R1"),
            new CreatePhoenixDatasetRequest.DatasetExampleInput("Q2", null)));
    when(phoenixDatasetService.createDataset(eq("ds"), eq("desc"), any(), any()))
        .thenReturn(new PhoenixDatasetSummary("id-1", "ds", 2));

    PhoenixDatasetSummary out = experimentService.createPhoenixDataset(req);

    assertEquals("id-1", out.id());
    assertEquals(2, out.exampleCount());
  }

  @Test
  void createPhoenixDatasetUsesEmptyDescriptionWhenNull() {
    var req = new CreatePhoenixDatasetRequest(
        "ds",
        null,
        List.of(new CreatePhoenixDatasetRequest.DatasetExampleInput("Q", null)));
    when(phoenixDatasetService.createDataset(eq("ds"), eq(""), any(), any()))
        .thenReturn(new PhoenixDatasetSummary("x", "ds", 1));

    experimentService.createPhoenixDataset(req);

    verify(phoenixDatasetService).createDataset(eq("ds"), eq(""), any(), any());
  }

  @Test
  void deletePhoenixDatasetDelegates() {
    experimentService.deletePhoenixDataset("abc");
    verify(phoenixDatasetService).deleteDataset("abc");
  }

  @Test
  void listPhoenixDatasetsDelegates() {
    when(phoenixDatasetService.listDatasets()).thenReturn(List.of());
    assertTrue(experimentService.listPhoenixDatasets().isEmpty());
  }

  @Test
  void getRunReturnsDetailWhenFound() {
    OffsetDateTime t = OffsetDateTime.parse("2026-04-19T10:00:00Z");
    ExperimentRun run = ExperimentRun.builder()
        .id(1L)
        .name("N")
        .datasetName("D")
        .phoenixDatasetId("p")
        .phoenixBaseUrl("http://p")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("gpt-5.4-mini")
        .status(ExperimentRunStatus.COMPLETED)
        .totalExamples(1)
        .meanFaithfulness(0.9)
        .createdAt(t)
        .completedAt(t)
        .build();
    when(experimentRunRepository.findById(1L)).thenReturn(Optional.of(run));
    when(experimentResultRepository.findByExperimentRunIdOrderByIdAsc(1L)).thenReturn(List.of(
        ExperimentResult.builder()
            .id(10L)
            .question("q")
            .referenceAnswer("ref")
            .ragResponse("rag")
            .documents("x".repeat(600))
            .faithfulness(1.0)
            .relevance(1.0)
            .correctness(1.0)
            .conciseness(1.0)
            .faithfulnessExplanation("f")
            .relevanceExplanation("r")
            .correctnessExplanation("c")
            .concisenessExplanation("k")
            .build()));

    Optional<ExperimentRunDetailResponse> got = experimentService.getRun(1L);
    assertTrue(got.isPresent());
    assertEquals(1, got.get().results().size());
    assertTrue(got.get().results().getFirst().documentsPreview().endsWith("..."));
  }

  @Test
  void getStatusReturnsSummaryWhenFound() {
    ExperimentRun run = ExperimentRun.builder()
        .id(2L)
        .name("N")
        .datasetName("D")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("gpt-5.4-mini")
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(3)
        .createdAt(OffsetDateTime.parse("2026-04-19T10:00:00Z"))
        .build();
    when(experimentRunRepository.findById(2L)).thenReturn(Optional.of(run));

    assertTrue(experimentService.getStatus(2L).isPresent());
  }

  @Test
  void listRunsMapsRepositoryRows() {
    ExperimentRun run = ExperimentRun.builder()
        .id(3L)
        .name("Run")
        .datasetName("ds")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("gpt-5.4-mini")
        .status(ExperimentRunStatus.COMPLETED)
        .totalExamples(2)
        .createdAt(OffsetDateTime.parse("2026-04-19T10:00:00Z"))
        .completedAt(OffsetDateTime.parse("2026-04-19T10:10:00Z"))
        .build();
    when(experimentRunRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(run));

    var summaries = experimentService.listRuns();
    assertEquals(1, summaries.size());
    assertEquals("Run", summaries.getFirst().name());
  }

  @Test
  void startRunRejectsUnknownGeneratorModel() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    var req = new RunExperimentRequest("ds-1", "n", null, "unknown-model", "gpt-5.4-mini", null);
    assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunRejectsUnknownEvaluatorModel() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "unknown-eval", null);
    assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunRejectsUnconfiguredGenerator() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(false);

    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "claude-haiku-4-5-20251001", null);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
    assertTrue(ex.getMessage().contains("Generator model is not configured"));
  }

  @Test
  void startRunRejectsUnconfiguredEvaluator() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(any(SupportedChatModel.class)))
        .thenAnswer(inv -> inv.getArgument(0) == SupportedChatModel.GPT_5_4_MINI);

    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "claude-haiku-4-5-20251001", null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
    assertTrue(ex.getMessage().contains("Evaluator model is not configured"));
  }

  @Test
  void startRunRejectsSameProviderModels() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4)).thenReturn(true);

    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "gpt-5.4", null);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
    assertTrue(ex.getMessage().contains("different providers"));
  }

  @Test
  void startRunCapsExamplesWhenMaxExamplesSet() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(phoenixDatasetService.getExamples("ds-1")).thenReturn(List.of(
        new PhoenixDatasetExample("a", "r", null, null),
        new PhoenixDatasetExample("b", "r", null, null),
        new PhoenixDatasetExample("c", "r", null, null)));
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.CLAUDE_HAIKU_4_5)).thenReturn(true);
    when(phoenixProperties.getBaseUrl()).thenReturn("http://phoenix:6006");

    ExperimentRun saved = ExperimentRun.builder()
        .id(50L)
        .name("Experiment test")
        .datasetName("n")
        .phoenixDatasetId("ds-1")
        .phoenixBaseUrl("http://phoenix:6006")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("claude-haiku-4-5-20251001")
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(2)
        .build();
    when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(saved);

    experimentService.startRun(
        new RunExperimentRequest(
            "ds-1", "n", "Experiment test", "gpt-5.4-mini", "claude-haiku-4-5-20251001", 2));

    ArgumentCaptor<ExperimentRun> captor = ArgumentCaptor.forClass(ExperimentRun.class);
    verify(experimentRunRepository).save(captor.capture());
    assertEquals(2, captor.getValue().getTotalExamples());
  }
}
