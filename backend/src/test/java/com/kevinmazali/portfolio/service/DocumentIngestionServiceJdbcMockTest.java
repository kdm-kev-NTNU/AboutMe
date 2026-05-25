package com.kevinmazali.portfolio.service;

import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.DocumentIngestProperties;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.model.VectorStoreInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentIngestionService} with mocked {@link JdbcTemplate} (no real PostgreSQL).
 */
@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceJdbcMockTest {

  @Mock
  private VectorStore vectorStore;
  @Mock
  private JdbcTemplate jdbcTemplate;
  @Mock
  private VectorStoreProperties vectorStoreProperties;
  @Mock
  private org.springframework.beans.factory.ObjectProvider<PiiSanitizerService> piiSanitizerProvider;
  @Mock
  private DocumentIngestProperties documentIngestProperties;
  @Mock
  private DocumentRegistryService documentRegistryService;

  private DocumentIngestionService service;
  private final PgVectorStoreProperties pgVectorStoreProperties = new PgVectorStoreProperties();

  @BeforeEach
  void setUp() {
    SanitizerProperties sanitizerProperties = new SanitizerProperties();
    sanitizerProperties.setEnabled(false);
    pgVectorStoreProperties.setSchemaName("public");
    pgVectorStoreProperties.setTableName("vector_store");
    service = new DocumentIngestionService(
        vectorStore,
        jdbcTemplate,
        new ObjectMapper(),
        pgVectorStoreProperties,
        vectorStoreProperties,
        new NoiseCleaner(),
        piiSanitizerProvider,
        sanitizerProperties,
        documentIngestProperties,
        documentRegistryService);
  }

  @Test
  void describeCollectionsUsesRowCount() {
    when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(42L);
    VectorStoreInfoResponse resp = service.describeCollections();
    assertEquals("vector_store", resp.activeCollectionName());
    assertEquals(42L, resp.activeCollectionEmbeddingCount());
    assertEquals(1, resp.collections().size());
  }
}
