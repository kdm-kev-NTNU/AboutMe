package com.kevinmazali.portfolio.testsupport;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/** Test-only no-op vector store when pgvector autoconfig is excluded (H2 in-memory tests). */
public final class TestNoOpVectorStore implements VectorStore {

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
