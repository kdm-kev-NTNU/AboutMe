package com.kevinmazali.portfolio.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.service.PostHogFeatureFlagManagementClient;
import com.kevinmazali.portfolio.service.RealtimeModelCatalog;
import com.kevinmazali.portfolio.service.VoiceKillSwitch;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

@WebMvcTest(controllers = FeatureSwitchAdminController.class)
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class,
  RealtimeRateLimitProperties.class,
  AiBudgetProperties.class,
  AiKillSwitchProperties.class
})
@Import({
  WebConfig.class,
  SecurityConfig.class,
  MvcTestSessionAuthConfig.class,
  MvcTestUserDetailsConfig.class
})
class FeatureSwitchAdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private VoiceKillSwitch voiceKillSwitch;
  @MockitoBean private PostHogFeatureFlagManagementClient managementClient;
  @MockitoBean private RealtimeModelCatalog realtimeModelCatalog;

  @Test
  @WithMockUser(roles = "ADMIN")
  void voiceStatusReturnsSnapshot() throws Exception {
    when(voiceKillSwitch.snapshot())
        .thenReturn(
            new VoiceKillSwitch.Snapshot(
                VoiceKillSwitch.State.NOT_ENGAGED,
                false,
                VoiceKillSwitch.Source.POSTHOG,
                Instant.parse("2026-09-19T05:00:00Z"),
                "aboutme_voice_kill_switch"));
    when(realtimeModelCatalog.hasAvailableModels()).thenReturn(true);
    when(managementClient.isConfigured()).thenReturn(true);
    when(voiceKillSwitch.isPosthogReadConfigured()).thenReturn(true);
    when(managementClient.posthogFlagUrl())
        .thenReturn("https://eu.posthog.com/project/162788/feature_flags/281659");

    mockMvc
        .perform(get("/admin/tools/features/voice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.killSwitchEngaged").value(false))
        .andExpect(jsonPath("$.liveEnabled").value(true))
        .andExpect(jsonPath("$.capability").value(true))
        .andExpect(jsonPath("$.flagKey").value("aboutme_voice_kill_switch"));
  }

  @Test
  void voiceStatusForbiddenForAnonymous() throws Exception {
    mockMvc.perform(get("/admin/tools/features/voice")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void killSwitchRequiresEngagedField() throws Exception {
    mockMvc
        .perform(
            post("/admin/tools/features/voice/kill-switch")
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void killSwitchReturns503WhenPostHogNotConfigured() throws Exception {
    when(managementClient.isConfigured()).thenReturn(false);
    mockMvc
        .perform(
            post("/admin/tools/features/voice/kill-switch")
                .contentType(APPLICATION_JSON)
                .content("{\"engaged\": true}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("POSTHOG_NOT_CONFIGURED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void killSwitchEngagesAndUpdatesCache() throws Exception {
    when(managementClient.isConfigured()).thenReturn(true);
    doNothing().when(managementClient).setKillSwitchEngaged(true);
    when(voiceKillSwitch.snapshot())
        .thenReturn(
            new VoiceKillSwitch.Snapshot(
                VoiceKillSwitch.State.ENGAGED,
                true,
                VoiceKillSwitch.Source.MANAGEMENT_WRITE,
                Instant.parse("2026-09-19T05:00:00Z"),
                "aboutme_voice_kill_switch"));
    when(realtimeModelCatalog.hasAvailableModels()).thenReturn(true);
    when(voiceKillSwitch.isPosthogReadConfigured()).thenReturn(true);
    when(managementClient.posthogFlagUrl()).thenReturn("https://eu.posthog.com/project/162788/feature_flags");

    mockMvc
        .perform(
            post("/admin/tools/features/voice/kill-switch")
                .contentType(APPLICATION_JSON)
                .content("{\"engaged\": true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.killSwitchEngaged").value(true))
        .andExpect(jsonPath("$.liveEnabled").value(false));

    verify(managementClient).setKillSwitchEngaged(true);
    verify(voiceKillSwitch)
        .applyKnownState(eq(true), eq(VoiceKillSwitch.Source.MANAGEMENT_WRITE));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void killSwitchReturns502WhenPostHogWriteFails() throws Exception {
    when(managementClient.isConfigured()).thenReturn(true);
    doThrow(new RestClientException("upstream"))
        .when(managementClient)
        .setKillSwitchEngaged(anyBoolean());

    mockMvc
        .perform(
            post("/admin/tools/features/voice/kill-switch")
                .contentType(APPLICATION_JSON)
                .content("{\"engaged\": false}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("POSTHOG_WRITE_FAILED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void killSwitchReturns400WhenAllowListRejects() throws Exception {
    when(managementClient.isConfigured()).thenReturn(true);
    doThrow(new IllegalArgumentException("Refusing to manage flag"))
        .when(managementClient)
        .setKillSwitchEngaged(anyBoolean());

    mockMvc
        .perform(
            post("/admin/tools/features/voice/kill-switch")
                .contentType(APPLICATION_JSON)
                .content("{\"engaged\": true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }
}
