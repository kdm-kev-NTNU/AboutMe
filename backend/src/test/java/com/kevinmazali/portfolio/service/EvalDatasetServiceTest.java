package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.repository.EvalDatasetExampleRepository;
import com.kevinmazali.portfolio.repository.EvalDatasetRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EvalDatasetServiceTest {

  @Mock
  private EvalDatasetRepository datasetRepository;

  @Mock
  private EvalDatasetExampleRepository exampleRepository;

  private EvalDatasetService service;

  @BeforeEach
  void setUp() {
    service = new EvalDatasetService(datasetRepository, exampleRepository);
  }

  @Test
  void createDatasetRejectsEmptyExamples() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createDataset(new CreateEvalDatasetRequest("n", "d", List.of())));
  }

  @Test
  void createDatasetRejectsBlankQuestion() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.createDataset(
                new CreateEvalDatasetRequest(
                    "n", null, List.of(new CreateEvalDatasetRequest.DatasetExampleInput("  ", null)))));
  }
}
