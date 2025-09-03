package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.crypto.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Bygger/loader SimpleVectorStore.
 * - Ved første gangs bygging: laster kilder, splitter, (valgfritt) krypterer innhold, og lagrer.
 * - Ved eksisterende fil: loader direkte (dekryptering skjer ved retrieval i service-laget).
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

  private final VectorStoreProperties props;

  public VectorStoreConfig(VectorStoreProperties props) {
    this.props = props;
  }

  @Bean
  public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
    Objects.requireNonNull(props.getVectorStorePath(), "sfg.aiapp.vectorStorePath må settes");
    File vectorStoreFile = new File(props.getVectorStorePath());
    ensureParentDirectoryExists(vectorStoreFile);

    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

    if (vectorStoreFile.exists()) {
      log.info("Laster eksisterende vector store fra: {}", vectorStoreFile.getAbsolutePath());
      store.load(vectorStoreFile);
      return store;
    }

    List<Resource> sources = props.getDocumentsToLoad();
    if (sources == null || sources.isEmpty()) {
      log.warn("Ingen dokumentkilder i sfg.aiapp.documentsToLoad – lagrer tom store på {}", vectorStoreFile.getAbsolutePath());
      store.save(vectorStoreFile);
      return store;
    }

    // Opprett CryptoService hvis kryptering er på
    CryptoService crypto = null;
    if (props.isEncryptContent()) {
      byte[] key = resolveKeyBytes(props.getEncryptionKeyBase64(), System.getenv("VECTORSTORE_ENC_KEY"));
      crypto = new CryptoService(key);
      log.info("Vector-store kryptering aktivert (AES-GCM).");
    } else {
      log.info("Vector-store kryptering er AV.");
    }

    log.info("Initialiserer vector store – henter {} kilder ...", sources.size());
    for (Resource res : sources) {
      try {
        for (Resource materialized : materialize(res)) {
          log.debug("Laster dokument: {}", describe(materialized));
          TikaDocumentReader documentReader = new TikaDocumentReader(materialized);
          List<Document> docs = documentReader.get();

          TextSplitter splitter = new TokenTextSplitter();
          List<Document> splitDocs = splitter.apply(docs);

          List<Document> toAdd = new ArrayList<>(splitDocs.size());
          for (Document d : splitDocs) {
            if (crypto != null) {
              CryptoService.EncResult enc = crypto.encrypt(d.getText());
              Map<String, Object> md = new LinkedHashMap<>(d.getMetadata());
              md.put("enc", "aesgcm");
              md.put("enc_iv", enc.ivBase64());
              // (valgfritt) marker versjon
              md.put("enc_v", "1");
              Document encDoc = new Document(enc.cipherBase64(), md);
              toAdd.add(encDoc);
            } else {
              toAdd.add(d);
            }
          }

          store.add(toAdd);
        }
      } catch (Exception e) {
        log.error("Feil ved materialisering/lesing av '{}': {}", describe(res), e.toString());
      }
    }

    store.save(vectorStoreFile);
    log.info("Vector store lagret til {}", vectorStoreFile.getAbsolutePath());
    return store;
  }

  private void ensureParentDirectoryExists(File file) {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
      throw new IllegalStateException("Kunne ikke opprette mappe: " + parent.getAbsolutePath());
    }
  }

  /**
   * Hvis http(s): stream til tempfil for robust håndtering (Tika liker filer/streams).
   * Hvis file:/classpath: returner som er.
   */
  private List<Resource> materialize(Resource resource) throws Exception {
    if (resource instanceof UrlResource urlRes) {
      String protocol = urlRes.getURL().getProtocol();
      if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
        // last ned til tmp
        String path = urlRes.getURL().getPath();
        String suffix = guessSuffix(path);
        Path tmp = Files.createTempFile("seed-", suffix);
        tmp.toFile().deleteOnExit();
        try (InputStream in = urlRes.getInputStream()) {
          Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        return List.of(new org.springframework.core.io.FileSystemResource(tmp));
      }
    }
    return List.of(resource);
  }

  private String guessSuffix(String path) {
    int idx = path.lastIndexOf('.');
    if (idx >= 0 && idx < path.length() - 1) {
      return path.substring(idx);
    }
    String ct = URLConnection.guessContentTypeFromName(path);
    if (ct != null) {
      if (ct.contains("pdf")) return ".pdf";
      if (ct.contains("word")) return ".docx";
      if (ct.contains("jpeg")) return ".jpg";
      if (ct.contains("png")) return ".png";
      if (ct.contains("html")) return ".html";
    }
    return ".bin";
  }

  private String describe(Resource r) {
    try {
      if (r instanceof UrlResource ur) return ur.getURL().toExternalForm();
      return r.getDescription();
    } catch (Exception e) {
      return r.toString();
    }
  }

  private byte[] resolveKeyBytes(String propKeyBase64, String envKeyBase64) {
    String b64 = (propKeyBase64 != null && !propKeyBase64.isBlank())
        ? propKeyBase64
        : (envKeyBase64 != null && !envKeyBase64.isBlank() ? envKeyBase64 : null);

    if (b64 == null) {
      throw new IllegalStateException(
          "Kryptering er aktivert, men ingen nøkkel satt. Sett sfg.aiapp.encryptionKeyBase64 eller VECTORSTORE_ENC_KEY (base64 for 32 bytes).");
    }
    byte[] key = Base64.getDecoder().decode(b64);
    if (key.length != 32) {
      throw new IllegalStateException("VECTORSTORE_ENC_KEY / sfg.aiapp.encryptionKeyBase64 må være base64 for 32 bytes (AES-256).");
    }
    return key;
  }
}
