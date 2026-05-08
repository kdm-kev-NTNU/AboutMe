package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.testsupport.VectorStoreTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "spring.ai.anthropic.api-key=sk-ant-api03-test-placeholder-for-spring-context-only",
        "portfolio.chat.default-model-id=gpt-5.4-mini",
        "server.port=0",
    })
class SecurityConfigIT {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void openapiDocsPermitAll() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
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
