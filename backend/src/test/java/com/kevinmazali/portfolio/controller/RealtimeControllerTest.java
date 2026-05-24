package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.RealtimeLookupResponse;
import com.kevinmazali.portfolio.model.RealtimeLookupSnippet;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
import com.kevinmazali.portfolio.service.ElevenLabsRealtimeTokenService;
import com.kevinmazali.portfolio.service.RealtimeLookupService;
import com.kevinmazali.portfolio.service.RealtimeModelCatalog;
import com.kevinmazali.portfolio.service.RealtimeSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.service.SpeechSynthesisService;
import com.kevinmazali.portfolio.service.TranscriptionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RealtimeController.class)
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class,
  RealtimeRateLimitProperties.class,
  RealtimeProperties.class
})
@TestPropertySource(properties = {
  "spring.ai.openai.api-key=sk-test",
  "portfolio.realtime.enabled=true",
  "portfolio.ask-rate-limit.enabled=false",
  "portfolio.realtime-rate-limit.enabled=false"
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class })
class RealtimeControllerTest {

  @Autowired
  private RealtimeProperties realtimeProperties;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RealtimeSessionService realtimeSessionService;

  @MockitoBean
  private RealtimeLookupService realtimeLookupService;

  @MockitoBean
  private RealtimeModelCatalog realtimeModelCatalog;

  @MockitoBean
  private ElevenLabsRealtimeTokenService elevenLabsRealtimeTokenService;

  @MockitoBean
  private TranscriptionService transcriptionService;

  @MockitoBean
  private SpeechSynthesisService speechSynthesisService;

  @MockitoBean
  private RequestLogService requestLogService;

  @AfterEach
  void restoreRealtimeDefaults() {
    realtimeProperties.setEnabled(true);
  }

  @Test
  void statusEnabledWhenFlagAndKey() throws Exception {
    when(realtimeModelCatalog.hasAvailableModels()).thenReturn(true);
    when(transcriptionService.isTranscriptionConfigured()).thenReturn(true);
    when(speechSynthesisService.isConfigured()).thenReturn(true);

    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.standardEnabled").value(true))
        .andExpect(jsonPath("$.liveEnabled").value(true))
        .andExpect(jsonPath("$.voices[0]").value("marin"))
        .andExpect(jsonPath("$.voices[1]").value("cedar"))
        .andExpect(jsonPath("$.reasoningEfforts[0]").value("low"))
        .andExpect(jsonPath("$.reasoningEfforts[1]").value("medium"))
        .andExpect(jsonPath("$.reasoningEfforts[2]").value("high"))
        .andExpect(jsonPath("$.defaultVoice").value("marin"))
        .andExpect(jsonPath("$.defaultReasoningEffort").value("low"));
  }

  @Test
  void sessionReturnsSdpAnswer() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any())).thenReturn("v=0\r\no=-");

    mockMvc.perform(post("/realtime/session")
            .content("v=0\r\no=offer")
            .contentType("application/sdp")
            .header("X-Chat-Language", "en")
            .header("X-Realtime-Voice", "cedar")
            .header("X-Realtime-Reasoning-Effort", "medium"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/sdp"))
        .andExpect(content().string("v=0\r\no=-"));

    verify(realtimeSessionService).createRealtimeCall(eq("v=0\r\no=offer"), eq("en"), isNull(), eq("cedar"), eq("medium"));
  }

  @Test
  void statusDisabledWhenFeatureFlagFalse() throws Exception {
    realtimeProperties.setEnabled(false);
    when(realtimeModelCatalog.hasAvailableModels()).thenReturn(false);
    when(transcriptionService.isTranscriptionConfigured()).thenReturn(false);
    when(speechSynthesisService.isConfigured()).thenReturn(false);
    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.standardEnabled").value(false))
        .andExpect(jsonPath("$.liveEnabled").value(false));
  }

  @Test
  void modelsEndpointReturnsConfiguredVoiceCatalog() throws Exception {
    when(realtimeModelCatalog.listAvailableModels())
        .thenReturn(List.of(
            new RealtimeModelOption("OPENAI", "gpt-realtime-2", "OpenAI GPT-Realtime-2", true),
            new RealtimeModelOption("ELEVENLABS", "agent_123", "ElevenLabs Agent", false)));

    mockMvc.perform(get("/realtime/models"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].provider").value("OPENAI"))
        .andExpect(jsonPath("$[0].id").value("gpt-realtime-2"))
        .andExpect(jsonPath("$[1].provider").value("ELEVENLABS"))
        .andExpect(jsonPath("$[1].id").value("agent_123"));
  }

  @Test
  void sessionRejectsUnsupportedVoiceBeforeCallingSessionService() throws Exception {
    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp")
            .header("X-Realtime-Voice", "alloy"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verify(realtimeSessionService, never()).createRealtimeCall(any(), any(), any(), any(), any());
    verify(requestLogService, never()).save(any(), any(), any(), any());
  }

  @Test
  void sessionRejectsUnsupportedReasoningEffortBeforeCallingSessionService() throws Exception {
    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp")
            .header("X-Realtime-Reasoning-Effort", "xhigh"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verify(realtimeSessionService, never()).createRealtimeCall(any(), any(), any(), any(), any());
    verify(requestLogService, never()).save(any(), any(), any(), any());
  }

  @Test
  void sessionReturns503WhenFeatureDisabledWithoutCallingSessionService() throws Exception {
    realtimeProperties.setEnabled(false);

    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.code").value("REALTIME_DISABLED"));

    verify(realtimeSessionService, never()).createRealtimeCall(any(), any(), any(), any(), any());
    verify(requestLogService, never()).save(any(), any(), any(), any());
  }

  @Test
  void sessionReturns400WhenServiceRejectsOffer() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("bad sdp"));

    mockMvc.perform(post("/realtime/session")
            .content("x")
            .contentType(MediaType.TEXT_PLAIN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("bad sdp"))
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void sessionReturns502WhenServiceFailsOpenAi() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any()))
        .thenThrow(
            new RealtimeSessionException(
                HttpStatus.BAD_GATEWAY,
                RealtimeErrorCode.OPENAI_REJECTED,
                "OpenAI rejected the session: invalid model"));

    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.error").value("OpenAI rejected the session: invalid model"))
        .andExpect(jsonPath("$.code").value("OPENAI_REJECTED"));
  }

  @Test
  void sessionPassesNorwegianLanguageHeaderThrough() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any())).thenReturn("sdp-answer");

    mockMvc.perform(post("/realtime/session")
            .content("offer")
            .contentType("application/sdp")
            .header("X-Chat-Language", "NO"))
        .andExpect(status().isOk());

    verify(realtimeSessionService).createRealtimeCall(eq("offer"), eq("NO"), isNull(), isNull(), isNull());
    verify(requestLogService).save("/realtime/session", "POST", "sdp-bytes", null);
  }

  @Test
  void sessionRecordsRequestBeforeCallingServiceOnSuccessPath() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any())).thenReturn("ok");

    mockMvc.perform(post("/realtime/session")
            .content("v")
            .contentType("application/sdp"));

    verify(requestLogService).save("/realtime/session", "POST", "sdp-bytes", null);
    verify(realtimeSessionService).createRealtimeCall(eq("v"), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  void lookupReturnsSnippets() throws Exception {
    when(realtimeLookupService.lookup(eq("NTNU"), eq("en")))
        .thenReturn(new RealtimeLookupResponse(
            true,
            List.of(new RealtimeLookupSnippet("profile", "Data engineering", "Kevin studies at NTNU."))));

    mockMvc.perform(post("/realtime/lookup")
            .content("{\"query\":\"NTNU\",\"language\":\"en\"}")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.found").value(true))
        .andExpect(jsonPath("$.snippets[0].sourceType").value("profile"))
        .andExpect(jsonPath("$.snippets[0].title").value("Data engineering"));

    verify(realtimeLookupService).lookup(eq("NTNU"), eq("en"));
  }

  @Test
  void lookupStillWorksWhenRealtimeFlagDisabled() throws Exception {
    realtimeProperties.setEnabled(false);
    when(realtimeLookupService.lookup(eq("NTNU"), eq("en")))
        .thenReturn(new RealtimeLookupResponse(true, List.of()));

    mockMvc.perform(post("/realtime/lookup")
            .content("{\"query\":\"NTNU\",\"language\":\"en\"}")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.found").value(true));

    verify(realtimeLookupService).lookup(eq("NTNU"), eq("en"));
  }

  @Test
  void lookupReturns400WhenServiceRejectsQuery() throws Exception {
    when(realtimeLookupService.lookup(any(), any()))
        .thenThrow(new IllegalArgumentException("Lookup query is required."));

    mockMvc.perform(post("/realtime/lookup")
            .content("{\"query\":\"\",\"language\":\"en\"}")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.error").value("Lookup query is required."));
  }
}

/**
 * Separate slice: realtime enabled in YAML but Spring AI OpenAI API key intentionally empty.
 */
@WebMvcTest(controllers = RealtimeController.class)
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class,
  RealtimeRateLimitProperties.class,
  RealtimeProperties.class
})
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=",
    "portfolio.realtime.enabled=true",
    "portfolio.ask-rate-limit.enabled=false",
    "portfolio.realtime-rate-limit.enabled=false"
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class })
class RealtimeControllerMissingOpenAiKeyMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RealtimeSessionService realtimeSessionService;

  @MockitoBean
  private RealtimeLookupService realtimeLookupService;

  @MockitoBean
  private RealtimeModelCatalog realtimeModelCatalog;

  @MockitoBean
  private ElevenLabsRealtimeTokenService elevenLabsRealtimeTokenService;

  @MockitoBean
  private TranscriptionService transcriptionService;

  @MockitoBean
  private SpeechSynthesisService speechSynthesisService;

  @MockitoBean
  private RequestLogService requestLogService;

  @Test
  void statusDisabledWhenApiKeyUnset() throws Exception {
    when(realtimeModelCatalog.hasAvailableModels()).thenReturn(false);
    when(transcriptionService.isTranscriptionConfigured()).thenReturn(false);
    when(speechSynthesisService.isConfigured()).thenReturn(false);
    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.standardEnabled").value(false))
        .andExpect(jsonPath("$.liveEnabled").value(false));
  }

  @Test
  void sessionPropagatesApiKeyMissingFromService() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any(), any(), any(), any()))
        .thenThrow(
            new RealtimeSessionException(
                HttpStatus.SERVICE_UNAVAILABLE,
                RealtimeErrorCode.API_KEY_MISSING,
                "OpenAI API key is not configured."));

    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.code").value("API_KEY_MISSING"));

    verify(realtimeSessionService).createRealtimeCall(eq("v=0"), isNull(), isNull(), isNull(), isNull());
    verify(requestLogService).save("/realtime/session", "POST", "sdp-bytes", null);
  }

  @Test
  void elevenLabsTokenEndpointReturnsToken() throws Exception {
    when(elevenLabsRealtimeTokenService.createConversationToken("agent_123")).thenReturn("token_abc");

    mockMvc.perform(post("/realtime/elevenlabs/token")
            .content("{\"modelId\":\"agent_123\"}")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token_abc"));

    verify(requestLogService).save("/realtime/elevenlabs/token", "POST", "token", null);
  }

  @Test
  void elevenLabsTokenEndpointMapsRealtimeErrors() throws Exception {
    when(elevenLabsRealtimeTokenService.createConversationToken("agent_123"))
        .thenThrow(new RealtimeSessionException(
            HttpStatus.BAD_GATEWAY,
            RealtimeErrorCode.ELEVENLABS_REJECTED,
            "ElevenLabs rejected the session: invalid agent"));

    mockMvc.perform(post("/realtime/elevenlabs/token")
            .content("{\"modelId\":\"agent_123\"}")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("ELEVENLABS_REJECTED"))
        .andExpect(jsonPath("$.error").value("ElevenLabs rejected the session: invalid agent"));
  }
}
