package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ChromaHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated health check for ChromaDB (ops / load balancers).
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Operational health endpoints")
public class ChromaHealthController {

  private final ChromaApi chromaApi;
  private final ChromaVectorStoreProperties chromaStoreProperties;

  @Operation(summary = "ChromaDB health", description = "Returns whether the configured Chroma collection is reachable and embedding count.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Chroma reachable",
          content = @Content(schema = @Schema(implementation = ChromaHealthResponse.class))),
      @ApiResponse(responseCode = "503", description = "Chroma unreachable or collection missing",
          content = @Content(schema = @Schema(implementation = ChromaHealthResponse.class)))
  })
  @GetMapping("/chroma")
  public ResponseEntity<ChromaHealthResponse> chroma() {
    String tenant = chromaStoreProperties.getTenantName();
    String database = chromaStoreProperties.getDatabaseName();
    String collectionName = chromaStoreProperties.getCollectionName();
    try {
      ChromaApi.Collection col = chromaApi.getCollection(tenant, database, collectionName);
      if (col == null) {
        return ResponseEntity.status(503).body(new ChromaHealthResponse(
            false, collectionName, null, "Chroma collection not found: " + collectionName));
      }
      Long count = chromaApi.countEmbeddings(tenant, database, col.id());
      long safeCount = count == null ? 0L : count;
      return ResponseEntity.ok(new ChromaHealthResponse(true, collectionName, safeCount, null));
    } catch (Exception e) {
      return ResponseEntity.status(503).body(new ChromaHealthResponse(
          false, collectionName, null, e.getMessage()));
    }
  }
}
