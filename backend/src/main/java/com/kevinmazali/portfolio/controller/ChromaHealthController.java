package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ChromaHealthResponse;
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
public class ChromaHealthController {

  private final ChromaApi chromaApi;
  private final ChromaVectorStoreProperties chromaStoreProperties;

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
