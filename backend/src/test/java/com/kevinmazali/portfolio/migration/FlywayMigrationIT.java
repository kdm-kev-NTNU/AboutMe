package com.kevinmazali.portfolio.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@EnabledIf("com.kevinmazali.portfolio.migration.FlywayMigrationIT#dockerAvailable")
class FlywayMigrationIT {

  static boolean dockerAvailable() {
    return DockerClientFactory.instance().isDockerAvailable();
  }

  private static final DockerImageName PG_IMAGE =
      DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(PG_IMAGE).withDatabaseName("aboutme").withUsername("postgres").withPassword("postgres");

  @Test
  void flywayV1ThroughV14_createsTargetTables() throws Exception {
    try (var conn = POSTGRES.createConnection("");
        Statement st = conn.createStatement()) {
      st.execute("CREATE EXTENSION IF NOT EXISTS vector");
    }

    Flyway flyway =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();

    DataSource ds = flyway.getConfiguration().getDataSource();
    try (var conn = ds.getConnection();
        Statement st = conn.createStatement()) {
      assertTableExists(st, "documents");
      assertTableExists(st, "prompt_templates");
      assertTableExists(st, "experiment_metric_scores");
      assertTableExists(st, "ai_usage_events");
      assertColumnMissing(st, "experiment_runs", "dataset_name");
      assertColumnMissing(st, "experiment_results", "faithfulness");
      assertColumnExists(st, "experiment_results", "retrieved_context");
      assertTableMissing(st, "ai_usage_record");
    }
  }

  private static void assertTableExists(Statement st, String table) throws Exception {
    try (ResultSet rs =
        st.executeQuery(
            "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '"
                + table
                + "'")) {
      assertThat(rs.next()).as("table %s", table).isTrue();
    }
  }

  private static void assertTableMissing(Statement st, String table) throws Exception {
    try (ResultSet rs =
        st.executeQuery(
            "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '"
                + table
                + "'")) {
      assertThat(rs.next()).as("table %s should be absent", table).isFalse();
    }
  }

  private static void assertColumnExists(Statement st, String table, String column) throws Exception {
    try (ResultSet rs =
        st.executeQuery(
            "SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '"
                + table
                + "' AND column_name = '"
                + column
                + "'")) {
      assertThat(rs.next()).isTrue();
    }
  }

  private static void assertColumnMissing(Statement st, String table, String column) throws Exception {
    try (ResultSet rs =
        st.executeQuery(
            "SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '"
                + table
                + "' AND column_name = '"
                + column
                + "'")) {
      assertThat(rs.next()).isFalse();
    }
  }
}
