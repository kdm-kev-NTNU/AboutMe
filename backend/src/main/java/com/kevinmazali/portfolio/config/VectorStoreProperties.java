package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Konfig for vector store, seed-kilder og (valgfritt) innholdskryptering.
 */
@Configuration
@ConfigurationProperties(prefix = "sfg.aiapp")
public class VectorStoreProperties {

  /**
   * Full sti til lagret SimpleVectorStore (JSON).
   * Eksempel: /data/vectorstore.json
   */
  private String vectorStorePath;

  /**
   * Dokumentkilder som skal lastes ved førstegangsbygging.
   * Støtter file:, classpath:, http:, https:
   */
  private List<Resource> documentsToLoad;

  /**
   * Slå på/av kryptering av dokument-innhold i vector-store.
   * Default true.
   */
  private boolean encryptContent = true;

  /**
   * Base64-kodet AES-256 nøkkel (32 byte) for kryptering/dekryptering.
   * Kan også komme via miljøvariabel VECTORSTORE_ENC_KEY.
   * Hvis begge er satt, vinner denne propertyen.
   */
  private String encryptionKeyBase64;

  /**
   * Base-katalog for dokumenter som skal lastes.
   * Støtter classpath:, file:, etc.
   * Eksempel: classpath:/tmp/docs/
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
