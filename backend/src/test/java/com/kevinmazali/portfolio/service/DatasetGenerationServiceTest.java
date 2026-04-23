package com.kevinmazali.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import com.kevinmazali.portfolio.model.experiment.GenerateDatasetRequest;
import com.kevinmazali.portfolio.repository.DatasetGenerationRepository;
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
