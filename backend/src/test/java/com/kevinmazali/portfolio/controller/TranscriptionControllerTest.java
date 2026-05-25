package com.kevinmazali.portfolio.controller;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import com.kevinmazali.portfolio.MockConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.controller.advice.GlobalApiExceptionHandler;
import com.kevinmazali.portfolio.config.ApiErrorConfiguration;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TranscriptionController.class)
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
class TranscriptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TranscriptionService transcriptionService;

  @Test
  void transcribeRejectsEmptyFile() throws Exception {
    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "a.webm", "audio/webm", new byte[0])))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void transcribeRejectsUnsupportedContentType() throws Exception {
    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] { 1, 2, 3 })))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void transcribeReturnsText() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), isNull())).thenReturn("hello");

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("hello"));

    verify(transcriptionService).transcribe(any(), anyLong(), isNull());
  }

  @Test
  void transcribePassesLanguageHeader() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), org.mockito.ArgumentMatchers.eq("no"))).thenReturn("hei");

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes()))
            .header("X-Chat-Language", "no"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("hei"));

    verify(transcriptionService).transcribe(any(), anyLong(), org.mockito.ArgumentMatchers.eq("no"));
  }

  @Test
  void transcribeReturns429WhenBudgetExceeded() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), isNull())).thenThrow(new BudgetExceededException("over"));

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes())))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("over"));
  }

  @Test
  void transcribeReturns503WhenUnavailable() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), isNull())).thenThrow(new IllegalStateException("no key"));

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void transcribeReturns500AsStructuredJsonWhenServiceThrowsRuntimeException() throws Exception {
    // Spring AI / OpenAI transports can throw RestClientException, etc. The controller does not
    // catch RuntimeException itself, so it must reach GlobalApiExceptionHandler and become a JSON
    // 500 ApiError -- not Spring Boot's default HTML error page.
    when(transcriptionService.transcribe(any(), anyLong(), isNull()))
        .thenThrow(new RuntimeException("openai 502 bad gateway"));

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes())))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void transcribeReturns503WhenCircuitOpen() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), isNull()))
        .thenThrow(new AiCircuitOpenException("circuit"));

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("circuit"));
  }

  @Test
  void transcribePropagates400WhenServiceRejectsOversizedFile() throws Exception {
    // The controller delegates final size validation to the service (which knows the precise
    // 25 MiB bound). An IllegalArgumentException from the service must surface as 400 — not 500.
    when(transcriptionService.transcribe(any(), anyLong(), isNull()))
        .thenThrow(new IllegalArgumentException("Audio file exceeds maximum size of 25 MB."));

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "big.webm", "audio/webm", new byte[] { 1, 2, 3 })))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Audio file exceeds maximum size of 25 MB."));
  }

  @Test
  void transcribeIgnoresInvalidLanguageHeader() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), isNull())).thenReturn("ok");

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes()))
            .header("X-Chat-Language", "fr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("ok"));

    // The unsupported "fr" must be normalised away to null so the service never sees it.
    verify(transcriptionService).transcribe(any(), anyLong(), isNull());
  }

  @Test
  void transcribeForwardsEnglishLanguageHeader() throws Exception {
    when(transcriptionService.transcribe(any(), anyLong(), eq("en"))).thenReturn("hello");

    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "r.webm", "audio/webm", "abc".getBytes()))
            .header("X-Chat-Language", "EN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("hello"));

    verify(transcriptionService).transcribe(any(), anyLong(), eq("en"));
  }

  @Test
  void transcribeReturns400OnUnsupportedLikeAudio() throws Exception {
    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void transcribeRejectsMissingContentType() throws Exception {
    mockMvc.perform(multipart("/transcribe")
            .file(new MockMultipartFile("file", "x.bin", null, new byte[] { 1 })))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }
}
