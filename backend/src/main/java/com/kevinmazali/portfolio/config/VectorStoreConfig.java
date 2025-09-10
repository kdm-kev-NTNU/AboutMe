package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.crypto.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.env.Environment;
import org.springframework.boot.system.ApplicationHome;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Configuration responsible for initializing and maintaining the vector store.
 *
 * <p>On startup it will try to load an existing store from disk; if none exists,
 * it will discover, parse, optionally encrypt, chunk, embed and persist
 * documents as a new {@link SimpleVectorStore}.</p>
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

  @Value("${sfg.aiapp.documentsToLoad:}")
  private String documentsToLoadFromYaml;

  /**
   * Creates and initializes the {@link SimpleVectorStore} bean.
   *
   * @param embeddingModel the embedding model used to embed chunks
   * @param vectorStoreProperties configuration properties for the vector store
   * @param env Spring environment for optional fallbacks
   * @return a loaded or freshly built {@link SimpleVectorStore}
   */
  @Bean
  public SimpleVectorStore simpleVectorStore(
      EmbeddingModel embeddingModel,
      VectorStoreProperties vectorStoreProperties,
      Environment env
  ) throws IOException {

    // Build store with the embedding model
    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

    // Startup log: which embedding model and dimensions are in use
    try {
      String modelClass = embeddingModel.getClass().getName();
      String modelName = "(ukjent)";
      String modelDimensions = "(ukjent)";
      try {
        java.lang.reflect.Method getDefaultOptions = embeddingModel.getClass().getMethod("getDefaultOptions");
        Object options = getDefaultOptions.invoke(embeddingModel);
        if (options != null) {
          try {
            java.lang.reflect.Method getModel = options.getClass().getMethod("getModel");
            Object mn = getModel.invoke(options);
            if (mn != null) modelName = String.valueOf(mn);
          } catch (NoSuchMethodException ignored) { }
          try {
            java.lang.reflect.Method getDimensions = options.getClass().getMethod("getDimensions");
            Object dim = getDimensions.invoke(options);
            if (dim != null) modelDimensions = String.valueOf(dim);
          } catch (NoSuchMethodException ignored) { }
        }
      } catch (NoSuchMethodException ignored) { }
      // Fallback: obtain from Spring properties if reflection does not provide it
      if ("(ukjent)".equals(modelName)) {
        String propModel = env.getProperty("spring.ai.openai.embedding.options.model");
        if (propModel != null && !propModel.isBlank()) {
          modelName = propModel;
        }
      }
      if ("(ukjent)".equals(modelDimensions)) {
        String propDims = env.getProperty("spring.ai.openai.embedding.options.dimensions");
        if (propDims != null && !propDims.isBlank()) {
          modelDimensions = propDims;
        }
      }
      log.info("Embedding model in use: class='{}', model='{}', dimensions={}", modelClass, modelName, modelDimensions);
    } catch (Exception e) {
      log.warn("Kunne ikke logge embedding-modell detaljer: {}", e.getMessage());
    }

    // File used to save/load the vector store (always anchored under 'backend')
    File vectorStoreFile = resolveVectorStoreFilePath(vectorStoreProperties.getVectorStorePath());
    ensureParentDir(vectorStoreFile);

    if (vectorStoreFile.exists()) {
      log.info("Laster eksisterende vector store fra: {}", vectorStoreFile.getPath());
      store.load(vectorStoreFile);
      return store;
    }

    log.info("Ingen eksisterende vector store. Leser og indekserer dokumenter ...");

    // --- Load documents ---
    List<Resource> resources = resolveResources(vectorStoreProperties);

    if (resources.isEmpty()) {
      log.warn("Fant ingen dokumenter å laste. Sjekk 'documentsToLoad' / basekatalog og filendelser.");
    } else {
      log.info("Fant {} dokument(er) til indeksering.", resources.size());
    }

    TextSplitter textSplitter = new TokenTextSplitter();

    for (Resource res : resources) {
      try {
        log.debug("Leser dokument: {}", safeName(res));
        TikaDocumentReader reader = new TikaDocumentReader(res);
        List<Document> docs = reader.get();
        
        if (docs == null || docs.isEmpty()) {
          log.warn("Ingen dokumenter funnet i '{}' - hopper over", safeName(res));
          continue;
        }
        
        // Process documents and add metadata for content type
        List<Document> processedDocs = processMultimodalDocuments(docs, res);
        
        if (processedDocs.isEmpty()) {
          log.warn("Ingen prosesserte dokumenter fra '{}' - hopper over", safeName(res));
          continue;
        }
        
        List<Document> splitDocs = textSplitter.apply(processedDocs);
        
        if (splitDocs == null || splitDocs.isEmpty()) {
          log.warn("Ingen split-dokumenter generert fra '{}' - hopper over", safeName(res));
          continue;
        }
        
        // Encrypt documents when encryption is enabled
        if (vectorStoreProperties.isEncryptContent()) {
          CryptoService crypto = createCryptoService(vectorStoreProperties);
          if (crypto != null) {
            splitDocs = encryptDocuments(splitDocs, crypto);
            log.debug("Kryptert {} dokumenter fra '{}'", splitDocs.size(), safeName(res));
          } else {
            log.warn("Kryptering er aktivert men ingen nøkkel funnet - lagrer ukryptert");
          }
        }
        
        store.add(splitDocs);
        log.debug("Lagt til {} dokumenter fra '{}'", splitDocs.size(), safeName(res));
      } catch (Exception e) {
        log.error("Feil ved lesing/indeksering av '{}': {}", safeName(res), e.getMessage(), e);
      }
    }

    // Persist the newly built vector store
    store.save(vectorStoreFile);
    log.info("Vector store lagret til: {}", vectorStoreFile.getPath());

    return store;
  }

  // --- Helpers ---

  /** Ensures the parent directory of the given file exists. */
  private static void ensureParentDir(File file) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      Files.createDirectories(parent.toPath());
    }
  }

  /**
   * Løser konfigurasjonens sti til en ABSOLUTT sti forankret under katalogen 'backend'.
   * Hvis 'backend' ikke finnes i mappestrukturen ved runtime (f.eks. i Docker),
   * forankres stien under applikasjonens hjemmekatalog (samme katalog som jar/klasser).
   */
  /**
   * Resolves the configured store path to an absolute path anchored under the
   * 'backend' directory when possible, or the application's home directory as fallback.
   */
  private File resolveVectorStoreFilePath(String configuredPath) {
    if (configuredPath == null || configuredPath.isBlank()) {
      configuredPath = "vectordatabase/vectorstore.json";
    }

    File configuredFile = new File(configuredPath);
    if (configuredFile.isAbsolute()) {
      return configuredFile;
    }

    String relative = stripLeadingDotSlash(configuredPath);

    // Find the application home directory (jar/classes folder)
    File appHome = new ApplicationHome(VectorStoreConfig.class).getDir();

    // Walk upwards to find a directory named 'backend'
    File current = appHome;
    while (current != null) {
      if ("backend".equalsIgnoreCase(current.getName())) {
        return new File(current, relative);
      }
      current = current.getParentFile();
    }

    // Alternatively, check if 'backend' exists relative to user.dir
    File userDir = new File(System.getProperty("user.dir", "."));
    File backendDir = "backend".equalsIgnoreCase(userDir.getName()) ? userDir : new File(userDir, "backend");
    if (backendDir.exists() && backendDir.isDirectory()) {
      return new File(backendDir, relative);
    }

    // Fallback: anchor under the application's home directory
    return new File(appHome, relative);
  }

  /** Removes a leading "./" prefix for normalization. */
  private static String stripLeadingDotSlash(String path) {
    String p = path.trim();
    if (p.startsWith("./")) {
      return p.substring(2);
    }
    return p;
  }

  /**
   * Løfter enten en eksplisitt liste av Resource (hvis konfigurert),
   * ellers bruker basekatalog (classpath:/ eller file:) og matcher på endelser.
   */
  /**
   * Resolves input resources either from a pre-configured list, from the vector
   * store folder (seeding), or by scanning a base directory for supported extensions.
   */
  private List<Resource> resolveResources(VectorStoreProperties props) throws IOException {
    List<Resource> result = new ArrayList<>();

    // 0) If a list of Resource is already provided in props, use it directly.
    if (props.getDocumentsToLoad() != null && !props.getDocumentsToLoad().isEmpty()) {
      result.addAll(props.getDocumentsToLoad());
      return result;
    }

    // 1) Try to find seed files in the same folder as the vector store (anchored under 'backend')
    try {
      File vectorStoreFile = resolveVectorStoreFilePath(props.getVectorStorePath());
      File vectorStoreDir = vectorStoreFile.getParentFile();
      if (vectorStoreDir != null && vectorStoreDir.exists() && vectorStoreDir.isDirectory()) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<String> exts = Arrays.asList("pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

        String baseUri = vectorStoreDir.toURI().toString(); // e.g. file:/app/vectordatabase/
        for (String ext : exts) {
          String pattern = baseUri + "**/*." + ext;
          try {
            Resource[] found = resolver.getResources(pattern);
            if (found.length > 0) {
              result.addAll(Arrays.asList(found));
            }
          } catch (Exception e) {
            log.debug("Skipping scan for .{} in vectorStoreDir due to: {}", ext, e.getMessage());
          }
        }

        if (!result.isEmpty()) {
          log.info("Loading seed files from vector store directory: {}", vectorStoreDir.getAbsolutePath());
          if (log.isInfoEnabled()) {
            result.forEach(r -> log.info(" - {}", safeName(r)));
          }
          return result; // Prioritize vector database directory first
        }
      }
    } catch (Exception e) {
      log.debug("Klarte ikke å skanne vector store katalog for seed-filer: {}", e.getMessage());
    }

    // 2) Else: read base dir from props, with @Value fallback from YAML
    String baseDir = props.getDocumentsToLoadDir();

    if ((baseDir == null || baseDir.isBlank()) && documentsToLoadFromYaml != null && !documentsToLoadFromYaml.isBlank()) {
      baseDir = documentsToLoadFromYaml;
    }

    if (baseDir == null || baseDir.isBlank()) {
      log.warn("Ingen documentsToLoad(base-dir) angitt. Sett f.eks. 'documentsToLoad: classpath:/tmp/docs/' i YAML.");
      return result;
    }

    // Normalize – ensure it ends with '/'
    if (!baseDir.endsWith("/")) {
      baseDir = baseDir + "/";
    }

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    // If baseDir is classpath:* and does not exist, avoid noise by skipping the scan
    try {
      if (baseDir.startsWith("classpath:")) {
        Resource base = resolver.getResource(baseDir);
        if (!base.exists()) {
          log.warn("No files found in '{}' with extensions {} (base dir does not exist)", baseDir, extsToString());
          return result;
        }
      }
    } catch (Exception ignored) { }

    // Search recursively in subfolders: **/*.ext
    List<String> exts = Arrays.asList("pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");
    for (String ext : exts) {
      String pattern = baseDir + "**/*." + ext; // e.g. classpath:/tmp/docs/**/*.(pdf/docx/...)
      try {
        Resource[] found = resolver.getResources(pattern);
        result.addAll(Arrays.asList(found));
        if (found.length > 0) {
          log.debug("Found {} files with extension .{}", found.length, ext);
        }
      } catch (Exception e) {
        log.debug("Skipped search for .{} in '{}': {}", ext, baseDir, e.getMessage());
      }
    }

    if (result.isEmpty()) {
      log.warn("No files found in '{}' with extensions {}", baseDir, extsToString());
    } else {
      if (log.isInfoEnabled()) {
        log.info("Files to be loaded from '{}':", baseDir);
        result.forEach(r -> log.info(" - {}", safeName(r)));
      }
    }

    return result;
  }

  /** Returns a human-friendly name for logging. */
  private static String safeName(Resource r) {
    try { return r.getURL().toString(); }
    catch (Exception e) { return r.getFilename(); }
  }

  /** Returns the supported file extensions as a log-friendly string. */
  private static String extsToString() {
    return "[pdf, docx, doc, txt, md, png, jpg, jpeg, gif, bmp, tiff, webp, svg]";
  }

  /**
   * Processes documents and enriches with content-type metadata.
   * Handles both text and image content with appropriate metadata.
   */
  /**
   * Adds content-type, filename and source metadata to documents; handles images and text.
   */
  private List<Document> processMultimodalDocuments(List<Document> documents, Resource resource) {
    List<Document> processedDocs = new ArrayList<>();
    
    for (Document doc : documents) {
      try {
        String filename = resource.getFilename();
        if (filename == null) {
          processedDocs.add(doc);
          continue;
        }
        
        String lowerFilename = filename.toLowerCase();
        
        // Check whether the file is an image
        if (lowerFilename.endsWith(".png") || lowerFilename.endsWith(".jpg") || 
            lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".gif") || 
            lowerFilename.endsWith(".bmp") || lowerFilename.endsWith(".tiff") || 
            lowerFilename.endsWith(".webp") || lowerFilename.endsWith(".svg")) {
          
          // For images: add metadata indicating image content
          Document imageDoc = new Document(doc.getText(), doc.getMetadata());
          imageDoc.getMetadata().put("content_type", "image");
          imageDoc.getMetadata().put("filename", filename);
          imageDoc.getMetadata().put("source", safeName(resource));
          processedDocs.add(imageDoc);
          
        } else {
          // For text documents: add metadata indicating text content
          Document textDoc = new Document(doc.getText(), doc.getMetadata());
          textDoc.getMetadata().put("content_type", "text");
          textDoc.getMetadata().put("filename", filename);
          textDoc.getMetadata().put("source", safeName(resource));
          processedDocs.add(textDoc);
        }
        
      } catch (Exception e) {
        log.warn("Error while processing document from '{}': {}", safeName(resource), e.getMessage());
        processedDocs.add(doc); // Fallback to original document
      }
    }
    
    return processedDocs;
  }

  /**
   * Creates CryptoService from configuration.
   */
  /** Builds a {@link CryptoService} from properties or environment variables. */
  private CryptoService createCryptoService(VectorStoreProperties props) {
    try {
      String keyBase64 = props.getEncryptionKeyBase64();
      if (keyBase64 == null || keyBase64.isBlank()) {
        // Try environment variable as fallback
        keyBase64 = System.getenv("VECTORSTORE_ENC_KEY");
      }
      
      if (keyBase64 == null || keyBase64.isBlank()) {
        log.warn("No encryption key found in configuration or VECTORSTORE_ENC_KEY environment variable");
        return null;
      }
      
      byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
      return new CryptoService(keyBytes);
    } catch (Exception e) {
      log.error("Error creating CryptoService: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Encrypts documents and adds metadata.
   */
  /** Encrypts document text and adds encryption metadata when possible. */
  private List<Document> encryptDocuments(List<Document> documents, CryptoService crypto) {
    return documents.stream()
        .map(doc -> {
          try {
            String originalText = doc.getText();
            if (originalText == null || originalText.trim().isEmpty()) {
              return doc; // Do not encrypt empty text
            }
            
            CryptoService.EncResult encrypted = crypto.encrypt(originalText);
            
            // Create new document with encrypted text and metadata
            Document encryptedDoc = new Document(encrypted.cipherBase64(), doc.getMetadata());
            encryptedDoc.getMetadata().put("enc", "aesgcm");
            encryptedDoc.getMetadata().put("enc_iv", encrypted.ivBase64());
            
            return encryptedDoc;
          } catch (Exception e) {
            log.error("Error encrypting document: {}", e.getMessage(), e);
            return doc; // Return original document on error
          }
        })
        .toList();
  }
}