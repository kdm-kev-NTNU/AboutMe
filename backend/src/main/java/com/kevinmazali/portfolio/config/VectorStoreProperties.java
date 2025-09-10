package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Configuration properties for the vector store, document seeding sources,
 * and optional content encryption.
 */
@Configuration
@ConfigurationProperties(prefix = "sfg.aiapp")
public class VectorStoreProperties {

  /**
   * Absolute or relative path to the persisted {@code SimpleVectorStore} JSON file.
   * Example: /data/vectorstore.json
   */
  private String vectorStorePath;

  /**
   * Document sources to ingest on first startup. Supports file:, classpath:, http:, https:.
   */
  private List<Resource> documentsToLoad;

  /**
   * Enables encryption of document content stored in the vector store. Default: true.
   */
  private boolean encryptContent = true;

  /**
   * Base64-encoded AES-256 key (32 bytes) used for encryption/decryption.
   * Can alternatively be provided via the VECTORSTORE_ENC_KEY environment variable.
   * When both are present, this property takes precedence.
   */
  private String encryptionKeyBase64;

  /**
   * Base directory to scan for documents to ingest. Supports classpath:, file:, etc.
   * Example: classpath:/tmp/docs/
   */
  private String documentsToLoadDir;

  public String getVectorStorePath() {
    return vectorStorePath;
  }

  public void setVectorStorePath(String vectorStorePath) {
    this.vectorStorePath = vectorStorePath;
  }

  public List<Resource> getDocumentsToLoad() {
    return documentsToLoad;
  }

  public void setDocumentsToLoad(List<Resource> documentsToLoad) {
    this.documentsToLoad = documentsToLoad;
  }

  public boolean isEncryptContent() {
    return encryptContent;
  }

  public void setEncryptContent(boolean encryptContent) {
    this.encryptContent = encryptContent;
  }

  public String getEncryptionKeyBase64() {
    return encryptionKeyBase64;
  }

  public void setEncryptionKeyBase64(String encryptionKeyBase64) {
    this.encryptionKeyBase64 = encryptionKeyBase64;
  }

  public String getDocumentsToLoadDir() {
    return documentsToLoadDir;
  }

  public void setDocumentsToLoadDir(String documentsToLoadDir) {
    this.documentsToLoadDir = documentsToLoadDir;
  }
}
