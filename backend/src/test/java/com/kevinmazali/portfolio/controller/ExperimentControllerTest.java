package com.kevinmazali.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.experiment.CreatePhoenixDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.service.ExperimentService;
import com.kevinmazali.portfolio.service.PhoenixDatasetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExperimentController.class)
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class })
class ExperimentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ExperimentService experimentService;

  @MockBean
  private PhoenixDatasetService phoenixDatasetService;

  @MockBean
  private ChatModelCatalog chatModelCatalog;

  @MockBean
  private PhoenixProperties phoenixProperties;

  @Test
  void runsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/admin/tools/experiments/runs"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void runsReturnsListForAdmin() throws Exception {
    when(experimentService.listRuns()).thenReturn(List.of(
        new ExperimentRunSummaryResponse(
            1L,
            "Run A",
            "ds",
            "gpt-5.4-mini",
            "gpt-5.4-mini",
            ExperimentRunStatus.COMPLETED,
            3,
            0.9,
            0.8,
            0.7,
            0.6,
            null,
            OffsetDateTime.parse("2026-04-19T10:00:00Z"),
            OffsetDateTime.parse("2026-04-19T10:05:00Z"))));

    mockMvc.perform(get("/admin/tools/experiments/runs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void configExposesPhoenixFlags() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(phoenixProperties.getBaseUrl()).thenReturn("http://localhost:6006");

    mockMvc.perform(get("/admin/tools/experiments/config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phoenixConfigured").value(true))
        .andExpect(jsonPath("$.phoenixBaseUrl").value("http://localhost:6006"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void datasetsReturns503WhenPhoenixDisabled() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(false);

    mockMvc.perform(get("/admin/tools/experiments/datasets"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void modelsEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/admin/tools/experiments/models"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void modelsEndpointReturnsCatalogForAdmin() throws Exception {
    when(chatModelCatalog.listAvailableModels()).thenReturn(List.of(
        new ChatModelOption("gpt-5.4-mini", ChatProvider.OPENAI, "GPT-5.4 mini")));

    mockMvc.perform(get("/admin/tools/experiments/models"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("gpt-5.4-mini"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void startRunReturns400WhenServiceRejects() throws Exception {
    when(experimentService.startRun(any(RunExperimentRequest.class)))
        .thenThrow(new IllegalArgumentException("bad request"));

    mockMvc.perform(post("/admin/tools/experiments/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "datasetId", "x",
                "generatorModel", "gpt-5.4-mini",
                "evaluatorModel", "gpt-5.4-mini"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("bad request"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void startRunReturnsAcceptedForAdmin() throws Exception {
    when(experimentService.startRun(any(RunExperimentRequest.class))).thenReturn(7L);

    mockMvc.perform(post("/admin/tools/experiments/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RunExperimentRequest(
                "ds-1", "n", null, "gpt-5.4-mini", "gpt-5.4-mini", null))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(7));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listDatasetsReturnsOkWhenPhoenixEnabled() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(experimentService.listPhoenixDatasets()).thenReturn(List.of(
        new PhoenixDatasetSummary("id-1", "My DS", 3)));

    mockMvc.perform(get("/admin/tools/experiments/datasets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("id-1"))
        .andExpect(jsonPath("$[0].exampleCount").value(3));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listDatasetsReturns502WhenListFails() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(experimentService.listPhoenixDatasets()).thenThrow(new RuntimeException("boom"));

    mockMvc.perform(get("/admin/tools/experiments/datasets"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.error", containsString("Phoenix list failed")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDatasetReturnsNoContent() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);

    mockMvc.perform(delete("/admin/tools/experiments/datasets/ds-1"))
        .andExpect(status().isNoContent());

    verify(experimentService).deletePhoenixDataset("ds-1");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDatasetReturns502WhenDeleteFails() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    org.mockito.Mockito.doThrow(new RuntimeException("nope")).when(experimentService).deletePhoenixDataset("x");

    mockMvc.perform(delete("/admin/tools/experiments/datasets/x"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.error", containsString("Phoenix delete failed")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturnsCreated() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(experimentService.createPhoenixDataset(any(CreatePhoenixDatasetRequest.class)))
        .thenReturn(new PhoenixDatasetSummary("new-id", "n", 1));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreatePhoenixDatasetRequest(
                "n",
                "d",
                List.of(new CreatePhoenixDatasetRequest.DatasetExampleInput("q?", "ref"))))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("new-id"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturns400OnValidationError() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(experimentService.createPhoenixDataset(any(CreatePhoenixDatasetRequest.class)))
        .thenThrow(new IllegalArgumentException("examples required"));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreatePhoenixDatasetRequest("n", null, List.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("examples required"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturns502OnUnexpectedError() throws Exception {
    when(phoenixDatasetService.isEnabled()).thenReturn(true);
    when(experimentService.createPhoenixDataset(any(CreatePhoenixDatasetRequest.class)))
        .thenThrow(new RuntimeException("upstream"));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreatePhoenixDatasetRequest(
                "n",
                null,
                List.of(new CreatePhoenixDatasetRequest.DatasetExampleInput("q", null))))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.error", containsString("Phoenix create failed")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRunReturnsDetailWhenPresent() throws Exception {
    var detail = new ExperimentRunDetailResponse(
        5L,
        "Run",
        "ds",
        "phx",
        "http://p",
        "gpt-5.4-mini",
        "gpt-5.4-mini",
        ExperimentRunStatus.COMPLETED,
        2,
        0.9,
        0.8,
        0.7,
        0.6,
        null,
        OffsetDateTime.parse("2026-04-19T10:00:00Z"),
        OffsetDateTime.parse("2026-04-19T10:05:00Z"),
        List.of());
    when(experimentService.getRun(5L)).thenReturn(Optional.of(detail));

    mockMvc.perform(get("/admin/tools/experiments/runs/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRunReturns404WhenMissing() throws Exception {
    when(experimentService.getRun(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/admin/tools/experiments/runs/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void statusReturnsSummaryWhenPresent() throws Exception {
    when(experimentService.getStatus(3L)).thenReturn(Optional.of(
        new ExperimentRunSummaryResponse(
            3L,
            "S",
            "ds",
            "gpt-5.4-mini",
            "gpt-5.4-mini",
            ExperimentRunStatus.RUNNING,
            1,
            null,
            null,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-04-19T10:00:00Z"),
            null)));

    mockMvc.perform(get("/admin/tools/experiments/runs/3/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void statusReturns404WhenMissing() throws Exception {
    when(experimentService.getStatus(8L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/admin/tools/experiments/runs/8/status"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void startRunReturns500OnUnexpectedException() throws Exception {
    when(experimentService.startRun(any(RunExperimentRequest.class)))
        .thenThrow(new RuntimeException("internal"));

    mockMvc.perform(post("/admin/tools/experiments/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RunExperimentRequest(
                "ds-1", "n", null, "gpt-5.4-mini", "gpt-5.4-mini", null))))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("internal"));
  }
}
