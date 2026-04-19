package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.CreatePhoenixDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.repository.ExperimentResultRepository;
import com.kevinmazali.portfolio.repository.ExperimentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    var req = new RunExperimentRequest("ds-1", "n", null, "gpt-5.4-mini", "gpt-5.4-mini", null);
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> experimentService.startRun(req));
  }

  @Test
  void startRunPersistsRunAndTriggersAsync() {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(phoenixDatasetService.getExamples("ds-1")).thenReturn(
        List.of(new PhoenixDatasetExample("What?", "Ref", null, null)));
    when(chatModelCatalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI)).thenReturn(true);
    when(phoenixProperties.getBaseUrl()).thenReturn("http://phoenix:6006");

    ExperimentRun saved = ExperimentRun.builder()
        .id(42L)
        .name("Experiment test")
        .datasetName("n")
        .phoenixDatasetId("ds-1")
        .phoenixBaseUrl("http://phoenix:6006")
        .generatorModel("gpt-5.4-mini")
        .evaluatorModel("gpt-5.4-mini")
        .status(ExperimentRunStatus.RUNNING)
        .totalExamples(1)
        .build();
    when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(saved);

    long id = experimentService.startRun(
        new RunExperimentRequest("ds-1", "n", "Experiment test", "gpt-5.4-mini", "gpt-5.4-mini", null));

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
}
