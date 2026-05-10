package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Focused JDBC/query behaviour for {@link DocumentIngestionService#getChunks}; full ingestion stays
 * excluded from JaCoCo until integration coverage exists.
 */
@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceGetChunksTest {

  @Mock private VectorStore vectorStore;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private NoiseCleaner noiseCleaner;
  @Mock private ObjectProvider<PiiSanitizerService> piiSanitizerProvider;
  @Mock private SanitizerProperties sanitizerProperties;

  private final PgVectorStoreProperties pgVectorStoreProperties = new PgVectorStoreProperties();
  private final VectorStoreProperties vectorStoreProperties = new VectorStoreProperties();

  private DocumentIngestionService service;

  @BeforeEach
  void setUp() {
    pgVectorStoreProperties.setSchemaName("public");
    pgVectorStoreProperties.setTableName("vector_store");
    when(sanitizerProperties.isEnabled()).thenReturn(false);
    when(piiSanitizerProvider.getIfAvailable()).thenReturn(null);

    service =
        new DocumentIngestionService(
            vectorStore,
            jdbcTemplate,
            new ObjectMapper(),
            pgVectorStoreProperties,
            vectorStoreProperties,
            noiseCleaner,
            piiSanitizerProvider,
            sanitizerProperties);
  }

  @Test
  void getChunks_clampsLimitAndOffsetUsesGlobalListingSql() {
    when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(500L);
    ChunkItem item =
        new ChunkItem("id-1", "f.md", 0, "hello", Map.of("filename", "f.md", "chunk_index", 0));
    when(jdbcTemplate.query(
            contains("ORDER BY id"),
            any(RowMapper.class),
            eq(200),
            eq(0)))
        .thenReturn(List.of(item));

    ChunkListResponse res = service.getChunks(null, 9999, -3);

    assertThat(res.limit()).isEqualTo(200);
    assertThat(res.offset()).isZero();
    assertThat(res.total()).isEqualTo(500L);
    assertThat(res.totalMatching()).isEqualTo(500L);
    assertThat(res.chunks()).containsExactly(item);
  }

  @Test
  void getChunks_withDocumentFilterUsesFilteredCountsAndQuery() {
    when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(100L);
    when(jdbcTemplate.queryForObject(
            contains("metadata->>'document_id'"), eq(Long.class), eq("doc-a")))
        .thenReturn(7L);
    ChunkItem item =
        new ChunkItem("c1", "x.txt", 1, "body", Map.of("document_id", "doc-a", "chunk_index", 1));
    when(jdbcTemplate.query(
            contains("WHERE metadata->>'document_id'"),
            any(RowMapper.class),
            eq("doc-a"),
            eq(50),
            eq(10)))
        .thenReturn(List.of(item));

    ChunkListResponse res = service.getChunks(" doc-a ", 50, 10);

    assertThat(res.total()).isEqualTo(100L);
    assertThat(res.totalMatching()).isEqualTo(7L);
    assertThat(res.chunks()).containsExactly(item);
  }

  @Test
  void getChunks_propagatesWhenVectorTableUnreachable() {
    when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class)))
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("no table"));

    assertThatThrownBy(() -> service.getChunks(null, 10, 0))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot access vector table");
  }
}
