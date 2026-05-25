package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleRow;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.repository.EvalDatasetExampleRepository;
import com.kevinmazali.portfolio.repository.EvalDatasetRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Test
  void createDatasetPersistsExamplesAndReturnsSummary() {
    when(datasetRepository.save(any(EvalDatasetEntity.class)))
        .thenAnswer(
            inv -> {
              EvalDatasetEntity e = inv.getArgument(0);
              e.setId(9L);
              return e;
            });

    EvalDatasetSummary out =
        service.createDataset(
            new CreateEvalDatasetRequest(
                "  ds  ",
                null,
                List.of(new CreateEvalDatasetRequest.DatasetExampleInput("  q  ", "ref"))));

    assertEquals("9", out.id());
    assertEquals("ds", out.name());
    assertEquals(1, out.exampleCount());
    ArgumentCaptor<EvalDatasetEntity> cap = ArgumentCaptor.forClass(EvalDatasetEntity.class);
    verify(datasetRepository).save(cap.capture());
    assertEquals(1, cap.getValue().getExamples().size());
    assertEquals("q", cap.getValue().getExamples().get(0).getQuestion());
  }

  @Test
  void listDatasetsIncludesExampleCounts() {
    EvalDatasetEntity d = new EvalDatasetEntity();
    d.setId(1L);
    d.setName("a");
    when(datasetRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(d));
    when(exampleRepository.countByDataset_Id(1L)).thenReturn(42L);

    List<EvalDatasetSummary> rows = service.listDatasets();
    assertEquals(1, rows.size());
    assertEquals("1", rows.get(0).id());
    assertEquals(42, rows.get(0).exampleCount());
  }

  @Test
  void getExamplesMapsNullReferenceToEmptyString() {
    EvalDatasetExampleEntity row = new EvalDatasetExampleEntity();
    row.setId(99L);
    row.setQuestion("q1");
    row.setReferenceText(null);
    when(exampleRepository.findByDataset_IdOrderByIdAsc(3L)).thenReturn(List.of(row));

    List<EvalDatasetExampleRow> out = service.getExamples("3");
    assertEquals(1, out.size());
    assertEquals("q1", out.get(0).question());
    assertEquals("", out.get(0).referenceText());
  }

  @Test
  void deleteDatasetParsesId() {
    service.deleteDataset(" 12 ");
    verify(datasetRepository).deleteById(12L);
  }

  @Test
  void getExamplesRejectsInvalidId() {
    assertThrows(IllegalArgumentException.class, () -> service.getExamples("x"));
    assertThrows(IllegalArgumentException.class, () -> service.getExamples(" "));
  }
}
