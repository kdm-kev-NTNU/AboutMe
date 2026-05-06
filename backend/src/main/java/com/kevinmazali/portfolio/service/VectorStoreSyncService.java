package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.SyncProperties;
import com.kevinmazali.portfolio.model.VectorStoreSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies {@code vector_store} rows from a configured remote Postgres (e.g. Railway) into the
 * application's primary database using JDBC (embeddings preserved).
 */
@Slf4j
@Service
public class VectorStoreSyncService {

  private static final int UPSERT_BATCH = 200;

  private final JdbcTemplate localJdbcTemplate;
  private final PgVectorStoreProperties pgVectorStoreProperties;
  private final SyncProperties syncProperties;

  public VectorStoreSyncService(
      JdbcTemplate localJdbcTemplate,
      PgVectorStoreProperties pgVectorStoreProperties,
      SyncProperties syncProperties) {
    this.localJdbcTemplate = localJdbcTemplate;
    this.pgVectorStoreProperties = pgVectorStoreProperties;
    this.syncProperties = syncProperties;
  }

  /**
   * Reads all rows from the remote JDBC URL configured in {@link SyncProperties} and upserts them
   * into the local vector table.
   */
  public VectorStoreSyncResult syncFromRemote(boolean clean) {
    JdbcTemplate remoteJdbc = createRemoteJdbcTemplate();
    return doSync(remoteJdbc, clean);
  }

  /**
   * Package-private for tests with a mocked remote {@link JdbcTemplate}.
   */
  VectorStoreSyncResult doSync(JdbcTemplate remoteJdbc, boolean clean) {
    Instant start = Instant.now();
    String qualified = qualifiedVectorTable();
    String masked = maskJdbcUrlForDisplay(syncProperties.getSourceUrl());
    log.info("Starting vector_store sync from remote {} (clean={})", masked, clean);

    String selectSql = """
        SELECT id, content, metadata::text AS metadata, embedding::text AS embedding
        FROM %s
        """.formatted(qualified);

    List<VectorRow> rows;
    try {
      rows = remoteJdbc.query(selectSql, this::extractVectorRows);
    } catch (DataAccessException e) {
      throw new IllegalStateException("Failed to read remote vector_store: " + e.getMessage(), e);
    }

    if (clean) {
      try {
        localJdbcTemplate.execute("TRUNCATE TABLE " + qualified);
      } catch (DataAccessException e) {
        throw new IllegalStateException("Failed to truncate local vector_store: " + e.getMessage(), e);
      }
    }

    long synced = 0L;
    for (int i = 0; i < rows.size(); i += UPSERT_BATCH) {
      int end = Math.min(i + UPSERT_BATCH, rows.size());
      List<VectorRow> batch = rows.subList(i, end);
      upsertBatch(qualified, batch);
      synced += batch.size();
    }

    long ms = Duration.between(start, Instant.now()).toMillis();
    log.info("Vector_store sync finished: {} rows in {} ms (source={})", synced, ms, masked);
    return new VectorStoreSyncResult(synced, ms, masked, clean);
  }

  private List<VectorRow> extractVectorRows(ResultSet rs) throws SQLException {
    List<VectorRow> out = new ArrayList<>();
    while (rs.next()) {
      String id = rs.getString("id");
      if (id == null || id.isBlank()) {
        log.warn("Skipping remote vector_store row with blank id");
        continue;
      }
      String content = rs.getString("content");
      if (content == null) {
        content = "";
      }
      String metadata = rs.getString("metadata");
      if (metadata == null || metadata.isBlank()) {
        metadata = "{}";
      }
      String embedding = rs.getString("embedding");
      if (embedding == null || embedding.isBlank()) {
        log.warn("Skipping remote vector_store row without embedding, id={}", id);
        continue;
      }
      out.add(new VectorRow(id, content, metadata, embedding));
    }
    return out;
  }

  private void upsertBatch(String qualifiedTable, List<VectorRow> batch) {
    if (batch.isEmpty()) {
      return;
    }
    String upsert = """
        INSERT INTO %s (id, content, metadata, embedding)
        VALUES (?, ?, ?::jsonb, ?::vector)
        ON CONFLICT (id) DO UPDATE SET
          content = EXCLUDED.content,
          metadata = EXCLUDED.metadata,
          embedding = EXCLUDED.embedding
        """.formatted(qualifiedTable);
    try {
      localJdbcTemplate.batchUpdate(upsert, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
          VectorRow r = batch.get(i);
          ps.setString(1, r.id());
          ps.setString(2, r.content());
          ps.setString(3, r.metadata());
          ps.setString(4, r.embedding());
        }

        @Override
        public int getBatchSize() {
          return batch.size();
        }
      });
    } catch (DataAccessException e) {
      throw new IllegalStateException("Failed to upsert local vector_store: " + e.getMessage(), e);
    }
  }

  private JdbcTemplate createRemoteJdbcTemplate() {
    String url = syncProperties.getSourceUrl() == null ? "" : syncProperties.getSourceUrl().trim();
    if (url.isBlank()) {
      throw new IllegalArgumentException("Sync source URL is not configured.");
    }
    if (!url.startsWith("jdbc:postgresql:")) {
      throw new IllegalArgumentException("Sync source URL must be a jdbc:postgresql: URL.");
    }
    String user = syncProperties.getSourceUsername() == null ? "" : syncProperties.getSourceUsername().trim();
    if (user.isBlank()) {
      throw new IllegalArgumentException("Sync source username is not configured.");
    }
    String password = syncProperties.getSourcePassword() == null ? "" : syncProperties.getSourcePassword();

    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName("org.postgresql.Driver");
    ds.setUrl(url);
    ds.setUsername(user);
    ds.setPassword(password);
    JdbcTemplate jdbc = new JdbcTemplate(ds);
    jdbc.setQueryTimeout(0);
    try {
      jdbc.queryForObject("SELECT 1", Integer.class);
    } catch (DataAccessException e) {
      throw new IllegalStateException("Cannot connect to remote Postgres for sync: " + e.getMessage(), e);
    }
    return jdbc;
  }

  private String qualifiedVectorTable() {
    return pgVectorStoreProperties.getSchemaName() + "." + pgVectorStoreProperties.getTableName();
  }

  /**
   * Parses {@code jdbc:postgresql://host:port/db} (optional query string) for logs — no credentials.
   */
  static String maskJdbcUrlForDisplay(String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return "";
    }
    try {
      String rest = jdbcUrl.replaceFirst("^jdbc:postgresql://", "");
      int slash = rest.indexOf('/');
      String hostPort = slash > 0 ? rest.substring(0, slash) : rest;
      String db = "";
      if (slash >= 0 && slash < rest.length() - 1) {
        db = rest.substring(slash + 1);
      }
      int q = db.indexOf('?');
      if (q >= 0) {
        db = db.substring(0, q);
      }
      return db.isEmpty() ? hostPort : hostPort + "/" + db;
    } catch (RuntimeException e) {
      return "(configured)";
    }
  }

  private record VectorRow(String id, String content, String metadata, String embedding) {}
}
