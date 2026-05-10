package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import static com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus.COMPLETED;
import com.kevinmazali.portfolio.model.experiment.GenerateDatasetRequest;
import com.kevinmazali.portfolio.repository.DatasetGenerationRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetGenerationServiceTest {

  @Mock private DatasetGenerationRepository datasetGenerationRepository;
  @Mock private DatasetGenerationAsyncRunner datasetGenerationAsyncRunner;
  @Mock private ChatModelCatalog chatModelCatalog;

  @InjectMocks private DatasetGenerationService datasetGenerationService;

  @Test
  void startGenerationRejectsBlankName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            datasetGenerationService.startGeneration(
                new GenerateDatasetRequest("", null, null, "gpt-5.4-mini", 1, null, null)));
  }

  @Test
  void startGenerationRejectsWhitespaceOnlyName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            datasetGenerationService.startGeneration(
                new GenerateDatasetRequest("  \t ", null, null, "gpt-5.4-mini", 1, null, null)));
  }

  @Test
  void startGenerationRejectsUnknownModel() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            datasetGenerationService.startGeneration(
                new GenerateDatasetRequest("DS", null, null, "model-that-does-not-exist", 1, null, null)));
    verifyNoInteractions(datasetGenerationRepository);
    verifyNoInteractions(datasetGenerationAsyncRunner);
  }

  @Test
  void startGenerationRejectsUnconfiguredModel() {
    when(chatModelCatalog.isModelConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(false);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            datasetGenerationService.startGeneration(
                new GenerateDatasetRequest("DS", null, null, "gpt-5.4-mini", 1, null, null)));

    verifyNoInteractions(datasetGenerationRepository);
  }

  @Test
  void getGenerationStatusReturnsEmptyWhenMissing() {
    when(datasetGenerationRepository.findById(404L)).thenReturn(Optional.empty());

    assertTrue(datasetGenerationService.getGenerationStatus(404L).isEmpty());
  }

  @Test
  void getGenerationStatusMapsEntityFields() {
    DatasetGeneration gen =
        DatasetGeneration.builder()
            .id(3L)
            .status(COMPLETED)
            .questionsGenerated(5)
            .resultDatasetId(99L)
            .errorMessage(null)
            .createdAt(OffsetDateTime.parse("2025-01-01T10:00:00Z"))
            .completedAt(OffsetDateTime.parse("2025-01-01T10:05:00Z"))
            .build();
    when(datasetGenerationRepository.findById(3L)).thenReturn(Optional.of(gen));

    var res = datasetGenerationService.getGenerationStatus(3L).orElseThrow();

    assertThat(res.id()).isEqualTo(3L);
    assertThat(res.status()).isEqualTo(COMPLETED);
    assertThat(res.questionsGenerated()).isEqualTo(5);
    assertThat(res.resultDatasetId()).isEqualTo("99");
    assertThat(res.createdAt()).contains("2025-01-01");
    assertThat(res.completedAt()).contains("2025-01-01");
  }

  @Test
  void startGenerationSavesAndDispatchesAsync() {
    when(chatModelCatalog.isModelConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    DatasetGeneration saved = DatasetGeneration.builder().id(9L).build();
    when(datasetGenerationRepository.save(any(DatasetGeneration.class))).thenReturn(saved);

    long id =
        datasetGenerationService.startGeneration(
            new GenerateDatasetRequest("DS", "desc", null, "gpt-5.4-mini", 2, 10, 42));

    assertEquals(9L, id);
    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository).save(cap.capture());
    assertEquals(2, cap.getValue().getQuestionsPerChunk());
    assertEquals(10, cap.getValue().getMaxQuestions());
    verify(datasetGenerationAsyncRunner).executeGeneration(9L);
  }
}
