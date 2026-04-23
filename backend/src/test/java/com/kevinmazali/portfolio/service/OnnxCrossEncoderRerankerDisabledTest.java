package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RetrievalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ONNX reranker is optional at runtime; without a model file the first rerank falls back to merge order.
 */
class OnnxCrossEncoderRerankerDisabledTest {

  @Test
  void missingModelFallsBackToPassThroughOrder() {
    RetrievalProperties p = new RetrievalProperties();
    p.setRerankEnabled(true);
    p.setOnnxModelPath("/nonexistent/onnx/model.onnx");
    p.setTokenizerPath("");
    OnnxCrossEncoderReranker r = new OnnxCrossEncoderReranker(p, null);
    List<Document> docs =
        List.of(
            new Document("first", new HashMap<>()),
            new Document("second", new HashMap<>()));
    List<Document> out = r.rerank("q", docs, 1);
    assertEquals("first", out.get(0).getText());
  }
}
