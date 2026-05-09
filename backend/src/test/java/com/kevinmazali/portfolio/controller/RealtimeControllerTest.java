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
import com.kevinmazali.portfolio.service.RealtimeSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
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
  private RequestLogService requestLogService;

  @AfterEach
  void restoreRealtimeDefaults() {
    realtimeProperties.setEnabled(true);
  }

  @Test
  void statusEnabledWhenFlagAndKey() throws Exception {
    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  void sessionReturnsSdpAnswer() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any())).thenReturn("v=0\r\no=-");

    mockMvc.perform(post("/realtime/session")
            .content("v=0\r\no=offer")
            .contentType("application/sdp")
            .header("X-Chat-Language", "en"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/sdp"))
        .andExpect(content().string("v=0\r\no=-"));

    verify(realtimeSessionService).createRealtimeCall(eq("v=0\r\no=offer"), eq("en"));
  }

  @Test
  void statusDisabledWhenFeatureFlagFalse() throws Exception {
    realtimeProperties.setEnabled(false);
    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
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

    verify(realtimeSessionService, never()).createRealtimeCall(any(), any());
    verify(requestLogService, never()).save(any(), any(), any(), any());
  }

  @Test
  void sessionReturns400WhenServiceRejectsOffer() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any()))
        .thenThrow(new IllegalArgumentException("bad sdp"));

    mockMvc.perform(post("/realtime/session")
            .content("x")
            .contentType(MediaType.TEXT_PLAIN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("bad sdp"));
  }

  @Test
  void sessionReturns502WhenServiceFailsOpenAi() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any()))
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
    when(realtimeSessionService.createRealtimeCall(any(), any())).thenReturn("sdp-answer");

    mockMvc.perform(post("/realtime/session")
            .content("offer")
            .contentType("application/sdp")
            .header("X-Chat-Language", "NO"))
        .andExpect(status().isOk());

    verify(realtimeSessionService).createRealtimeCall(eq("offer"), eq("NO"));
    verify(requestLogService).save("/realtime/session", "POST", "sdp-bytes", null);
  }

  @Test
  void sessionRecordsRequestBeforeCallingServiceOnSuccessPath() throws Exception {
    when(realtimeSessionService.createRealtimeCall(any(), any())).thenReturn("ok");

    mockMvc.perform(post("/realtime/session")
            .content("v")
            .contentType("application/sdp"));

    verify(requestLogService).save("/realtime/session", "POST", "sdp-bytes", null);
    verify(realtimeSessionService).createRealtimeCall(eq("v"), isNull());
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
  private RequestLogService requestLogService;

  @Test
  void statusDisabledWhenApiKeyUnset() throws Exception {
    mockMvc.perform(get("/realtime/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void sessionRequiresApiKeyReturns503WithoutLogging() throws Exception {
    mockMvc.perform(post("/realtime/session")
            .content("v=0")
            .contentType("application/sdp"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.code").value("API_KEY_MISSING"));

    verify(realtimeSessionService, never()).createRealtimeCall(any(), any());
    verify(requestLogService, never()).save(any(), any(), any(), any());
  }
}
