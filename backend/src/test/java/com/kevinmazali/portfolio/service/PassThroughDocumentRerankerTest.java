package com.kevinmazali.portfolio.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassThroughDocumentRerankerTest {

  @Test
  void truncatesToTopNInOrder() {
    DocumentReranker r = new PassThroughDocumentReranker();
    List<Document> in =
        List.of(
            new Document("a", new HashMap<>()),
            new Document("b", new HashMap<>()),
            new Document("c", new HashMap<>()));
    assertEquals(List.of("a", "b"), r.rerank("q", in, 2).stream().map(Document::getText).toList());
  }

  @Test
  void returnsEmptyWhenNoCandidatesOrNonPositiveTopN() {
    DocumentReranker r = new PassThroughDocumentReranker();
    List<Document> one = List.of(new Document("only", new HashMap<>()));
    assertTrue(r.rerank("q", null, 2).isEmpty());
    assertTrue(r.rerank("q", List.of(), 2).isEmpty());
    assertTrue(r.rerank("q", one, 0).isEmpty());
  }
}
