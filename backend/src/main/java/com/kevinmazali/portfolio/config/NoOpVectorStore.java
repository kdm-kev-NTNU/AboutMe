package com.kevinmazali.portfolio.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * Used when {@link PortfolioChromaProperties#isEnabled()} is {@code false}: RAG runs without retrieval
 * (empty context) instead of failing at bean creation time.
 */
public final class NoOpVectorStore implements VectorStore {

  @Override
  public void add(List<Document> documents) {
    // intentionally no-op
  }

  @Override
  public void delete(List<String> idList) {
    // intentionally no-op
  }

  @Override
  public void delete(Filter.Expression filterExpression) {
    // intentionally no-op
  }

  @Override
  public List<Document> similaritySearch(SearchRequest request) {
    return List.of();
  }
}
