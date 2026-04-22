package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.VectorStoreHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated health check for the pgvector {@code vector_store} table (ops / load balancers).
 * {@code GET /health/chroma} is kept as a stable alias for existing monitors and frontends.
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Operational health endpoints")
public class VectorStoreHealthController {

  /** Public response when the vector table is unreachable (no internals in body). */
  static final String PUBLIC_VECTOR_STORE_DOWN = "Vector store is currently unavailable.";

  private final JdbcTemplate jdbcTemplate;
  private final PgVectorStoreProperties pgVectorStoreProperties;

  @Operation(summary = "Vector store health (legacy path)", description = "Alias of /health/vectorstore for backward compatibility.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Vector store reachable",
          content = @Content(schema = @Schema(implementation = VectorStoreHealthResponse.class))),
      @ApiResponse(responseCode = "503", description = "Vector store unreachable",
          content = @Content(schema = @Schema(implementation = VectorStoreHealthResponse.class)))
  })
  @GetMapping("/chroma")
  public ResponseEntity<VectorStoreHealthResponse> chromaLegacyAlias() {
    return vectorStoreHealth();
  }

  @Operation(summary = "Vector store health", description = "Returns whether the configured pgvector table is reachable and row count.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Vector store reachable",
          content = @Content(schema = @Schema(implementation = VectorStoreHealthResponse.class))),
      @ApiResponse(responseCode = "503", description = "Vector store unreachable",
          content = @Content(schema = @Schema(implementation = VectorStoreHealthResponse.class)))
  })
  @GetMapping("/vectorstore")
  public ResponseEntity<VectorStoreHealthResponse> vectorStoreHealth() {
    String tableLabel = pgVectorStoreProperties.getTableName();
    try {
      Long count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM " + qualifiedTable(),
          Long.class);
      long safe = count == null ? 0L : count;
      return ResponseEntity.ok(new VectorStoreHealthResponse(true, tableLabel, safe, null));
    } catch (DataAccessException e) {
      log.warn("Vector store health check failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new VectorStoreHealthResponse(false, tableLabel, null, PUBLIC_VECTOR_STORE_DOWN));
    }
  }

  private String qualifiedTable() {
    return pgVectorStoreProperties.getSchemaName() + "." + pgVectorStoreProperties.getTableName();
  }
}
