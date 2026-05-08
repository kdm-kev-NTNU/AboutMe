package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MockConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.controller.advice.GlobalApiExceptionHandler;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
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
  DatasetGenerateRateLimitProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class, MockConfig.class, GlobalApiExceptionHandler.class })
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
}
