package com.kevinmazali.portfolio.controller;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import com.kevinmazali.portfolio.MockConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.ApiErrorConfiguration;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.controller.advice.GlobalApiExceptionHandler;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.service.SpeechSynthesisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpeechSynthesisController.class)
@EnableConfigurationProperties({
    AskRateLimitProperties.class,
    ExperimentRunRateLimitProperties.class,
    DatasetGenerateRateLimitProperties.class,
    RealtimeRateLimitProperties.class
})
@Import({
    WebConfig.class,
    SecurityConfig.class, MvcTestSessionAuthConfig.class,
    MvcTestUserDetailsConfig.class,
    MockConfig.class,
    GlobalApiExceptionHandler.class,
    ApiErrorConfiguration.class
})
class SpeechSynthesisControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SpeechSynthesisService speechSynthesisService;

  @MockitoBean
  private RequestLogService requestLogService;

  @Test
  void synthesizeReturnsMp3Audio() throws Exception {
    when(speechSynthesisService.synthesize("Hello", "en")).thenReturn(new byte[] {1, 2, 3});

    mockMvc.perform(post("/synthesize")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Chat-Language", "en")
            .content("{\"text\":\"Hello\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("audio/mpeg"))
        .andExpect(content().bytes(new byte[] {1, 2, 3}));

    verify(speechSynthesisService).synthesize(eq("Hello"), eq("en"));
  }

  @Test
  void synthesizeReturns400OnValidationFailure() throws Exception {
    when(speechSynthesisService.synthesize("", "en"))
        .thenThrow(new IllegalArgumentException("Text is required."));

    mockMvc.perform(post("/synthesize")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Chat-Language", "en")
            .content("{\"text\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Text is required."));
  }

  @Test
  void synthesizeReturns503WhenUnavailable() throws Exception {
    when(speechSynthesisService.synthesize("Hei", "no"))
        .thenThrow(new IllegalStateException("OpenAI API key is not configured."));

    mockMvc.perform(post("/synthesize")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Chat-Language", "no")
            .content("{\"text\":\"Hei\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("Speech synthesis is temporarily unavailable."));
  }
}
