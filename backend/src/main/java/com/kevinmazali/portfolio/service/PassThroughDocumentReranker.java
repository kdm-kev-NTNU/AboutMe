package com.kevinmazali.portfolio.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Preserves merge order and only truncates to {@code topN} (no extra scoring).
 */
public final class PassThroughDocumentReranker implements DocumentReranker {

  @Override
  public List<Document> rerank(String query, List<Document> candidates, int topN) {
    if (candidates == null || candidates.isEmpty() || topN <= 0) {
      return List.of();
    }
    int n = Math.min(topN, candidates.size());
    return List.copyOf(candidates.subList(0, n));
  }
}
