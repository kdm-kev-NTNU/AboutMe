package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.ApiErrorConfiguration;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.interview.InterviewDocumentResponse;
import com.kevinmazali.portfolio.model.interview.InterviewSessionResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptResponse;
import com.kevinmazali.portfolio.service.InterviewDocumentService;
import com.kevinmazali.portfolio.service.InterviewRealtimeSessionService;
import com.kevinmazali.portfolio.service.InterviewSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InterviewController.class)
@Import({
  SecurityConfig.class,
  MvcTestSessionAuthConfig.class,
  MvcTestUserDetailsConfig.class,
  ApiErrorConfiguration.class
})
class InterviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InterviewDocumentService interviewDocumentService;
  @MockitoBean private InterviewSessionService interviewSessionService;
  @MockitoBean private InterviewRealtimeSessionService interviewRealtimeSessionService;
  @MockitoBean private RequestLogService requestLogService;

  @Test
  void uploadRequiresAdmin() throws Exception {
    mockMvc
        .perform(
            multipart("/admin/tools/interview/documents")
                .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {1, 2})))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void uploadDocumentOk() throws Exception {
    when(interviewDocumentService.storeMultipart(any(), eq("admin")))
        .thenReturn(
            new InterviewDocumentResponse("doc1", "cv.pdf", "application/pdf", 100, "admin", Instant.now()));

    mockMvc
        .perform(
            multipart("/admin/tools/interview/documents")
                .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {1, 2})))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("doc1"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void createSessionOk() throws Exception {
    when(interviewSessionService.createSession(any()))
        .thenReturn(
            new InterviewSessionResponse(
                "sess1", "doc1", "no", "ACTIVE", "marin", Instant.now(), null, null, null, null, List.of()));

    mockMvc
        .perform(
            post("/admin/tools/interview/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\":\"doc1\",\"language\":\"no\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("sess1"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void cleanTranscriptOk() throws Exception {
    when(interviewSessionService.cleanTranscript("t1"))
        .thenReturn(
            new InterviewTranscriptResponse(
                "t1", "sess1", "raw", "cleaned", "CLEANED", null, Instant.now(), Instant.now()));

    mockMvc
        .perform(post("/admin/tools/interview/transcripts/t1/clean"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cleanStatus").value("CLEANED"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void ingestTranscriptOk() throws Exception {
    when(interviewSessionService.ingestTranscript("t1", false))
        .thenReturn(new IngestionResult("hash1", "interview.md", 3, false, "OK"));

    mockMvc
        .perform(post("/admin/tools/interview/transcripts/t1/ingest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skipped").value(false));

    verify(interviewSessionService).ingestTranscript("t1", false);
  }
}
