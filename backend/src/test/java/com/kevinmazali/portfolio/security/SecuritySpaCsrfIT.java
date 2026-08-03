package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.model.interview.InterviewDocumentResponse;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.service.InterviewDocumentService;
import com.kevinmazali.portfolio.service.InterviewSessionService;
import com.kevinmazali.portfolio.testsupport.VectorStoreTestConfiguration;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract: SPA sends the raw {@code XSRF-TOKEN} cookie value as {@code X-XSRF-TOKEN}.
 * Requires CSRF enabled ({@code portfolio.test.disable-csrf=false}) unlike WebMvc slices.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(VectorStoreTestConfiguration.class)
@TestPropertySource(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
      "spring.datasource.url=jdbc:h2:mem:spacsrfit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.ai.openai.api-key=test-placeholder-key-for-context-tests-only",
      "spring.ai.openai.chat.enabled=true",
      "spring.ai.anthropic.api-key=test-anthropic-api-key-not-real",
      "portfolio.chat.default-model-id=gpt-5.4-mini",
      "portfolio.session.jwt-secret=test-jwt-secret-at-least-32-characters-long",
      "portfolio.test.disable-csrf=false",
      "server.port=0",
    })
class SecuritySpaCsrfIT {

  @MockitoBean private DocumentIngestionService documentIngestionService;
  @MockitoBean private InterviewDocumentService interviewDocumentService;
  @MockitoBean private InterviewSessionService interviewSessionService;

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void interviewTextPost_withMatchingRawCsrfCookieAndHeader_succeeds() throws Exception {
    when(interviewDocumentService.storeText(eq("Q1"), eq("q.md"), eq("admin")))
        .thenReturn(
            new InterviewDocumentResponse(
                "doc1", "q.md", "text/plain", 2, "admin", Instant.now()));

    Cookie xsrf = primeCsrfCookie();

    mockMvc
        .perform(
            post("/admin/tools/interview/documents/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Q1\",\"filename\":\"q.md\"}")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void interviewUploadPost_withMatchingRawCsrfCookieAndHeader_succeeds() throws Exception {
    when(interviewDocumentService.storeMultipart(any(), eq("admin")))
        .thenReturn(
            new InterviewDocumentResponse(
                "doc2", "q.md", "text/markdown", 5, "admin", Instant.now()));

    Cookie xsrf = primeCsrfCookie();
    MockMultipartFile file =
        new MockMultipartFile("file", "q.md", "text/markdown", "hello".getBytes());

    mockMvc
        .perform(
            multipart("/admin/tools/interview/documents")
                .file(file)
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void interviewTextPost_withoutCsrfHeader_isForbidden() throws Exception {
    Cookie xsrf = primeCsrfCookie();

    mockMvc
        .perform(
            post("/admin/tools/interview/documents/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Q1\",\"filename\":\"q.md\"}")
                .cookie(xsrf))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void interviewTextPost_withMismatchedCsrfHeader_isForbidden() throws Exception {
    Cookie xsrf = primeCsrfCookie();

    mockMvc
        .perform(
            post("/admin/tools/interview/documents/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Q1\",\"filename\":\"q.md\"}")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", "not-the-cookie-value"))
        .andExpect(status().isForbidden());
  }

  private Cookie primeCsrfCookie() throws Exception {
    MvcResult result = mockMvc.perform(get("/auth/me")).andExpect(status().isOk()).andReturn();
    Cookie xsrf = result.getResponse().getCookie("XSRF-TOKEN");
    assertNotNull(xsrf, "GET /auth/me must set XSRF-TOKEN for SPA CSRF priming");
    assertNotNull(xsrf.getValue());
    return xsrf;
  }
}
