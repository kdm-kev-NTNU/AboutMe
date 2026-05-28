package com.kevinmazali.portfolio.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.testsupport.VectorStoreTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(VectorStoreTestConfiguration.class)
@TestPropertySource(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "spring.datasource.url=jdbc:h2:mem:securityit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "server.port=0",
    })
class SecurityConfigIT {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @MockitoBean
  private DocumentIngestionService documentIngestionService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
  }

  @Test
  void openapiDocsPermitAll() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  /** Run with {@code -Dopenapi.export=true} to refresh {@code frontend/homepage/openapi/openapi.json}. */
  @Test
  void openApiSnapshotIncludesEndpointsAndOptionalExport() throws Exception {
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    JsonNode paths = MAPPER.readTree(body).get("paths");
    for (String path :
        new String[] {
          "/auth/login",
          "/auth/me",
          "/auth/logout",
          "/realtime/status",
          "/realtime/models",
          "/realtime/session",
          "/realtime/lookup",
          "/transcribe",
          "/admin/tools/experiments/config",
          "/admin/tools/experiments/datasets",
          "/admin/tools/experiments/runs",
          "/admin/tools/ai/status",
        }) {
      assertTrue(paths != null && paths.has(path), "Missing OpenAPI path: " + path);
    }

    if ("true".equals(System.getProperty("openapi.export"))) {
      Path out =
          Path.of("..", "frontend", "homepage", "openapi", "openapi.json").normalize().toAbsolutePath();
      Files.createDirectories(out.getParent());
      Files.writeString(out, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(body)));
    }
  }

  @Test
  void adminDocumentsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/admin/tools/documents")).andExpect(status().isUnauthorized());
  }

  @Test
  void feedbackPostPermitAllWithoutCsrf() throws Exception {
    mockMvc
        .perform(
            post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Great portfolio site\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void corsPreflightAllowsConfiguredOrigin() throws Exception {
    mockMvc
        .perform(
            options("/feedback")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
  }
}
