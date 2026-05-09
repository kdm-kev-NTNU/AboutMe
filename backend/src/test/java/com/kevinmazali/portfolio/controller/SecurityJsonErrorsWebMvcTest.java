package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.ApiErrorConfiguration;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.SyncProperties;
import com.kevinmazali.portfolio.service.DefaultQuestionSuggestionService;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.service.VectorStoreSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ensures admin routes return structured {@code ApiError} JSON for 401/403. */
@WebMvcTest(controllers = DocumentPipelineController.class)
@Import({
  SecurityConfig.class,
  MvcTestUserDetailsConfig.class,
  DocumentPipelineControllerAdvice.class,
  ApiErrorConfiguration.class
})
class SecurityJsonErrorsWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DocumentIngestionService documentIngestionService;

  @MockitoBean private DefaultQuestionSuggestionService defaultQuestionSuggestionService;

  @MockitoBean private VectorStoreSyncService vectorStoreSyncService;

  @MockitoBean private SyncProperties syncProperties;

  @Test
  void adminRoute_withoutCredentials_returns401ApiErrorJson() throws Exception {
    mockMvc
        .perform(get("/admin/tools/documents").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Authentication required"))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @WithMockUser(username = "user", roles = "USER")
  void adminRoute_withoutAdminRole_returns403ApiErrorJson() throws Exception {
    mockMvc
        .perform(get("/admin/tools/documents").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Access denied"))
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }
}
