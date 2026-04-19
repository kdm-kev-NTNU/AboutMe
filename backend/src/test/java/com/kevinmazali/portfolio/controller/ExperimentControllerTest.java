package com.kevinmazali.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse;
import com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
}
