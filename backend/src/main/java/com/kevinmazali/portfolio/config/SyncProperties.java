package com.kevinmazali.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional JDBC source for copying the pgvector {@code vector_store} table from a remote Postgres
 * (e.g. Railway) into the application's primary database (local dev).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "portfolio.sync")
public class SyncProperties {

  /**
   * When {@code false}, {@code POST /admin/tools/documents/sync-from-remote} returns 403.
   * Production profile sets this to {@code false}.
   */
  private boolean enabled = false;

  /** JDBC URL for the remote Postgres (e.g. {@code jdbc:postgresql://host:5432/railway}). */
  private String sourceUrl = "";

  private String sourceUsername = "";

  private String sourcePassword = "";
}
