package com.kevinmazali.portfolio.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Reranks retrieval candidates (e.g. cross-encoder scores) before building the RAG context.
 */
public interface DocumentReranker {

  /**
   * @param query user question (typically the original, not translated variants)
   * @param candidates merged unique documents from vector search
   * @param topN maximum documents to return
   * @return up to {@code topN} documents in descending relevance order
   */
  List<Document> rerank(String query, List<Document> candidates, int topN);
}
