package com.kevinmazali.portfolio.controller;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.ModelTag;
import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatusResponse;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
import com.kevinmazali.portfolio.model.experiment.GenerateDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.RunExperimentRequest;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.service.DatasetGenerationService;
import com.kevinmazali.portfolio.service.ExperimentService;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class,
  RealtimeRateLimitProperties.class
})
@Import({WebConfig.class, SecurityConfig.class, MvcTestSessionAuthConfig.class, MvcTestUserDetailsConfig.class})
class ExperimentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ExperimentService experimentService;

  @MockitoBean
  private ChatModelCatalog chatModelCatalog;

  @MockitoBean
  private DatasetGenerationService datasetGenerationService;

  @MockitoBean
  private PostHogProperties postHogProperties;

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
            "claude-haiku-4-5-20251001",
            ExperimentRunStatus.COMPLETED,
            3,
            0.9,
            0.8,
            0.7,
            0.6,
            0.95,
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
  void configExposesPosthogFlags() throws Exception {
    when(postHogProperties.isCaptureConfigured()).thenReturn(true);
    when(postHogProperties.getHost()).thenReturn("https://eu.i.posthog.com");

    mockMvc.perform(get("/admin/tools/experiments/config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posthogConfigured").value(true))
        .andExpect(jsonPath("$.posthogHost").value("https://eu.i.posthog.com"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listDatasetsReturnsOk() throws Exception {
    when(experimentService.listEvalDatasets()).thenReturn(List.of(
        new EvalDatasetSummary("1", "My DS", 3)));

    mockMvc.perform(get("/admin/tools/experiments/datasets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("1"))
        .andExpect(jsonPath("$[0].exampleCount").value(3));
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
        new ChatModelOption("gpt-5.4-mini", ChatProvider.OPENAI, "GPT-5.4 mini", EnumSet.of(ModelTag.FAST))));

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
                "1", "n", null, "gpt-5.4-mini", "claude-haiku-4-5-20251001", null))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.runId").value(7));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDatasetReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/admin/tools/experiments/datasets/1"))
        .andExpect(status().isNoContent());

    verify(experimentService).deleteEvalDataset("1");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDatasetReturns400WhenDeleteFails() throws Exception {
    org.mockito.Mockito.doThrow(new RuntimeException("nope")).when(experimentService).deleteEvalDataset("x");

    mockMvc.perform(delete("/admin/tools/experiments/datasets/x"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Delete failed")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturnsCreated() throws Exception {
    when(experimentService.createEvalDataset(any(CreateEvalDatasetRequest.class)))
        .thenReturn(new EvalDatasetSummary("1", "n", 1));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateEvalDatasetRequest(
                "n",
                "d",
                List.of(new CreateEvalDatasetRequest.DatasetExampleInput("q?", "ref"))))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("1"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturns400OnValidationError() throws Exception {
    when(experimentService.createEvalDataset(any(CreateEvalDatasetRequest.class)))
        .thenThrow(new IllegalArgumentException("examples required"));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateEvalDatasetRequest("n", null, List.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("examples required"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createDatasetReturns500OnUnexpectedError() throws Exception {
    when(experimentService.createEvalDataset(any(CreateEvalDatasetRequest.class)))
        .thenThrow(new RuntimeException("upstream"));

    mockMvc.perform(post("/admin/tools/experiments/datasets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateEvalDatasetRequest(
                "n",
                null,
                List.of(new CreateEvalDatasetRequest.DatasetExampleInput("q", null))))))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("upstream"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRunReturnsDetailWhenPresent() throws Exception {
    var detail = new ExperimentRunDetailResponse(
        5L,
        "Run",
        "ds",
        1L,
        "https://eu.i.posthog.com",
        "gpt-5.4-mini",
        "claude-haiku-4-5-20251001",
        ExperimentRunStatus.COMPLETED,
        2,
        0.9,
        0.8,
        0.7,
        0.6,
        0.95,
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
            "claude-haiku-4-5-20251001",
            ExperimentRunStatus.RUNNING,
            1,
            null,
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
                "1", "n", null, "gpt-5.4-mini", "claude-haiku-4-5-20251001", null))))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("internal"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void generateDatasetReturnsAccepted() throws Exception {
    when(datasetGenerationService.startGeneration(any(GenerateDatasetRequest.class))).thenReturn(42L);

    mockMvc.perform(post("/admin/tools/experiments/datasets/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new GenerateDatasetRequest(
                "My DS", "", null, "gpt-5.4-mini", 1, null, null))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.generationId").value(42))
        .andExpect(jsonPath("$.status").value(DatasetGenerationStatus.RUNNING));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void generateDatasetReturns400OnIllegalArgument() throws Exception {
    when(datasetGenerationService.startGeneration(any(GenerateDatasetRequest.class)))
        .thenThrow(new IllegalArgumentException("bad"));

    mockMvc.perform(post("/admin/tools/experiments/datasets/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new GenerateDatasetRequest(
                "n", null, null, "gpt-5.4-mini", null, null, null))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void generationStatusReturnsOk() throws Exception {
    when(datasetGenerationService.getGenerationStatus(7L))
        .thenReturn(
            Optional.of(
                new DatasetGenerationStatusResponse(
                    7L,
                    DatasetGenerationStatus.COMPLETED,
                    3,
                    "99",
                    null,
                    OffsetDateTime.parse("2026-04-19T10:00:00Z").toString(),
                    OffsetDateTime.parse("2026-04-19T10:05:00Z").toString())));

    mockMvc.perform(get("/admin/tools/experiments/datasets/generate/7/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.resultDatasetId").value("99"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void generationStatusReturns404WhenMissing() throws Exception {
    when(datasetGenerationService.getGenerationStatus(1L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/admin/tools/experiments/datasets/generate/1/status"))
        .andExpect(status().isNotFound());
  }
}
