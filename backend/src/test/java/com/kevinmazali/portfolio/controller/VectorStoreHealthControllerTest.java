package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VectorStoreHealthController.class)
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class })
class VectorStoreHealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private PgVectorStoreProperties pgVectorStoreProperties;

  @Test
  void chromaAliasReturnsOkWhenCountReadable() throws Exception {
    when(pgVectorStoreProperties.getTableName()).thenReturn("vector_store");
    when(pgVectorStoreProperties.getSchemaName()).thenReturn("public");
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(7L);

    mockMvc.perform(get("/health/chroma"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.healthy").value(true))
        .andExpect(jsonPath("$.collectionName").value("vector_store"))
        .andExpect(jsonPath("$.embeddingCount").value(7));
  }

  @Test
  void vectorstoreReturns503OnDataAccessException() throws Exception {
    when(pgVectorStoreProperties.getTableName()).thenReturn("vector_store");
    when(pgVectorStoreProperties.getSchemaName()).thenReturn("public");
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
        .thenThrow(new QueryTimeoutException("timeout"));

    mockMvc.perform(get("/health/vectorstore"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.healthy").value(false))
        .andExpect(jsonPath("$.message").value(VectorStoreHealthController.PUBLIC_VECTOR_STORE_DOWN));
  }

  @Test
  void vectorstoreTreatsNullCountAsZero() throws Exception {
    when(pgVectorStoreProperties.getTableName()).thenReturn("vector_store");
    when(pgVectorStoreProperties.getSchemaName()).thenReturn("public");
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

    mockMvc.perform(get("/health/vectorstore"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.embeddingCount").value(0));
  }
}
