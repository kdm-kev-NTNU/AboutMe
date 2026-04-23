package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retrieval and optional ONNX cross-encoder reranking after vector search.
 */
@ConfigurationProperties(prefix = "portfolio.retrieval")
public class RetrievalProperties {

  /**
   * When false, candidates are taken in merge order and truncated to {@link #contextTopK}
   * (legacy behaviour when defaults match the previous fixed limits).
   */
  private boolean rerankEnabled = false;

  /** Filesystem path to the cross-encoder ONNX model file (e.g. model.onnx from an Optimum export). */
  private String onnxModelPath = "";

  /**
   * Directory containing {@code tokenizer.json}, or a path to {@code tokenizer.json}.
   * When blank, the parent directory of {@link #onnxModelPath} is used.
   */
  private String tokenizerPath = "";

  /** Per expanded-query vector search width before merge/dedupe. */
  private int vectorTopK = 40;

  /** Maximum unique chunks kept after merge before reranking (or before truncation when rerank is off). */
  private int candidateLimit = 40;

  /** Chunks passed into the RAG prompt after rerank (or after truncation when rerank is off). */
  private int contextTopK = 40;

  /** Cross-encoder forward batch size. */
  private int rerankBatchSize = 8;

  /** Character cap on passage text before tokenization (per chunk). */
  private int maxPassageChars = 2000;

  /** Tokenizer / ONNX sequence length cap (padding target for batched tensors). */
  private int maxSequenceLength = 256;

  public boolean isRerankEnabled() {
    return rerankEnabled;
  }

  public void setRerankEnabled(boolean rerankEnabled) {
    this.rerankEnabled = rerankEnabled;
  }

  public String getOnnxModelPath() {
    return onnxModelPath;
  }

  public void setOnnxModelPath(String onnxModelPath) {
    this.onnxModelPath = onnxModelPath;
  }

  public String getTokenizerPath() {
    return tokenizerPath;
  }

  public void setTokenizerPath(String tokenizerPath) {
    this.tokenizerPath = tokenizerPath;
  }

  public int getVectorTopK() {
    return vectorTopK;
  }

  public void setVectorTopK(int vectorTopK) {
    this.vectorTopK = vectorTopK;
  }

  public int getCandidateLimit() {
    return candidateLimit;
  }

  public void setCandidateLimit(int candidateLimit) {
    this.candidateLimit = candidateLimit;
  }

  public int getContextTopK() {
    return contextTopK;
  }

  public void setContextTopK(int contextTopK) {
    this.contextTopK = contextTopK;
  }

  public int getRerankBatchSize() {
    return rerankBatchSize;
  }

  public void setRerankBatchSize(int rerankBatchSize) {
    this.rerankBatchSize = rerankBatchSize;
  }

  public int getMaxPassageChars() {
    return maxPassageChars;
  }

  public void setMaxPassageChars(int maxPassageChars) {
    this.maxPassageChars = maxPassageChars;
  }

  public int getMaxSequenceLength() {
    return maxSequenceLength;
  }

  public void setMaxSequenceLength(int maxSequenceLength) {
    this.maxSequenceLength = maxSequenceLength;
  }
}
