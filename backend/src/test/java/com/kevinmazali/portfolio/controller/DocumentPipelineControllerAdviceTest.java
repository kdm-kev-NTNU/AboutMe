package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.SyncProperties;
import com.kevinmazali.portfolio.service.DefaultQuestionSuggestionService;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.service.VectorStoreSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link WebMvcTest} slice for {@link DocumentPipelineControllerAdvice} mappings on
 * {@link DocumentPipelineController} (503 + {@code ApiError} for ingestion I/O failures).
 */
@WebMvcTest(controllers = DocumentPipelineController.class)
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class, DocumentPipelineControllerAdvice.class })
class DocumentPipelineControllerAdviceTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DocumentIngestionService documentIngestionService;

  @MockitoBean
  private DefaultQuestionSuggestionService defaultQuestionSuggestionService;

  @MockitoBean
  private VectorStoreSyncService vectorStoreSyncService;

  @MockitoBean
  private SyncProperties syncProperties;

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void illegalState_mapsTo503() throws Exception {
    when(documentIngestionService.listDocuments())
        .thenThrow(new IllegalStateException("Cannot access vector table public.vector_store: boom"));

    mockMvc.perform(get("/admin/tools/documents"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(containsString("Cannot access vector table")));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void resourceAccess_mapsTo503() throws Exception {
    when(documentIngestionService.listDocuments())
        .thenThrow(new ResourceAccessException("Connection refused"));

    mockMvc.perform(get("/admin/tools/documents"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(containsString("Connection refused")));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void restClientException_mapsTo503() throws Exception {
    when(documentIngestionService.listDocuments())
        .thenThrow(new RestClientException("Bad gateway from upstream"));

    mockMvc.perform(get("/admin/tools/documents"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(containsString("Bad gateway from upstream")));
  }
}
