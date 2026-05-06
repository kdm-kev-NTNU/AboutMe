package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.SyncProperties;
import com.kevinmazali.portfolio.model.VectorStoreSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorStoreSyncServiceTest {

  @Mock
  private JdbcTemplate localJdbc;

  @Mock
  private JdbcTemplate remoteJdbc;

  private final PgVectorStoreProperties pgVectorStoreProperties = new PgVectorStoreProperties();
  private final SyncProperties syncProperties = new SyncProperties();
  private VectorStoreSyncService service;

  @BeforeEach
  void setUp() {
    pgVectorStoreProperties.setSchemaName("public");
    pgVectorStoreProperties.setTableName("vector_store");
    syncProperties.setSourceUrl("jdbc:postgresql://prod.example.com:5432/railwaydb");
    syncProperties.setSourceUsername("postgres");
    syncProperties.setSourcePassword("secret");
    service = new VectorStoreSyncService(localJdbc, pgVectorStoreProperties, syncProperties);
  }

  @Test
  void maskJdbcUrlForDisplay_stripsCredentialsAndQuery() {
    assertEquals(
        "db.example:6543/mydb",
        VectorStoreSyncService.maskJdbcUrlForDisplay("jdbc:postgresql://db.example:6543/mydb?sslmode=require"));
    assertEquals("localhost:5432", VectorStoreSyncService.maskJdbcUrlForDisplay("jdbc:postgresql://localhost:5432"));
    assertEquals("", VectorStoreSyncService.maskJdbcUrlForDisplay(""));
    assertEquals("", VectorStoreSyncService.maskJdbcUrlForDisplay(null));
  }

  @Test
  void doSync_truncatesWhenClean() throws Exception {
    when(remoteJdbc.query(contains("public.vector_store"), ArgumentMatchers.<ResultSetExtractor<?>>any()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          ResultSetExtractor<Object> ex = invocation.getArgument(1);
          ResultSet rs = mock(ResultSet.class);
          when(rs.next()).thenReturn(true, false);
          when(rs.getString("id")).thenReturn("chunk_1");
          when(rs.getString("content")).thenReturn("hello");
          when(rs.getString("metadata")).thenReturn("{\"k\":1}");
          when(rs.getString("embedding")).thenReturn("[0.1,0.2]");
          return ex.extractData(rs);
        });

    VectorStoreSyncResult result = service.doSync(remoteJdbc, true);

    verify(localJdbc).execute(contains("TRUNCATE"));
    verify(localJdbc).batchUpdate(contains("INSERT"), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
    assertEquals(1L, result.rowsSynced());
    assertEquals("prod.example.com:5432/railwaydb", result.sourceHostMasked());
    assertEquals(true, result.truncatedLocalFirst());
  }

  @Test
  void doSync_skipsTruncateWhenNotClean() throws Exception {
    when(remoteJdbc.query(contains("public.vector_store"), ArgumentMatchers.<ResultSetExtractor<?>>any()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          ResultSetExtractor<Object> ex = invocation.getArgument(1);
          ResultSet rs = mock(ResultSet.class);
          when(rs.next()).thenReturn(false);
          return ex.extractData(rs);
        });

    service.doSync(remoteJdbc, false);

    verify(localJdbc, never()).execute(contains("TRUNCATE"));
    verify(localJdbc, never()).batchUpdate(anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
  }

  @Test
  void syncFromRemote_rejectsNonPostgresUrl() {
    syncProperties.setSourceUrl("jdbc:h2:mem:test");
    assertThrows(IllegalArgumentException.class, () -> service.syncFromRemote(false));
  }

  @Test
  void syncFromRemote_requiresUsername() {
    syncProperties.setSourceUsername("  ");
    assertThrows(IllegalArgumentException.class, () -> service.syncFromRemote(false));
  }
}
