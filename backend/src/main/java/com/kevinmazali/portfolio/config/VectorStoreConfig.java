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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Configuration
public class VectorStoreConfig {

  // Fallback dersom VectorStoreProperties ikke har egen documentsToLoadDir
  @Value("${sfg.aiapp.documentsToLoad:}")
  private String documentsToLoadFromYaml; // f.eks. "classpath:/tmp/docs/"

  @Bean
  public SimpleVectorStore simpleVectorStore(
      EmbeddingModel embeddingModel,
      VectorStoreProperties vectorStoreProperties,
      Environment env
  ) throws IOException {

    // Bygg store med embedding model
    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

    // Oppstarts-logg: hvilken embedding-modell og dimensjoner som brukes
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
      // Fallback: hent fra Spring-properties dersom reflection ikke gir svar
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

    // Filen vi lagrer/laster vector store fra
    File vectorStoreFile = new File(vectorStoreProperties.getVectorStorePath());
    ensureParentDir(vectorStoreFile);

    if (vectorStoreFile.exists()) {
      log.info("Laster eksisterende vector store fra: {}", vectorStoreFile.getPath());
      store.load(vectorStoreFile);
      return store;
    }

    log.info("Ingen eksisterende vector store. Leser og indekserer dokumenter ...");

    // --- Hent dokumenter ---
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
        
        // Behandle dokumenter og legg til metadata for innholdstype
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
        
        // Krypter dokumenter hvis kryptering er aktivert
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

    // Lagre ny vector store
    store.save(vectorStoreFile);
    log.info("Vector store lagret til: {}", vectorStoreFile.getPath());

    return store;
  }

  // --- Helpers ---

  private static void ensureParentDir(File file) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      Files.createDirectories(parent.toPath());
    }
  }

  /**
   * Løfter enten en eksplisitt liste av Resource (hvis konfigurert),
   * ellers bruker basekatalog (classpath:/ eller file:) og matcher på endelser.
   */
  private List<Resource> resolveResources(VectorStoreProperties props) throws IOException {
    List<Resource> result = new ArrayList<>();

    // 1) Hvis du allerede har en liste av Resource i props (gammelt oppsett), bruk den direkte.
    if (props.getDocumentsToLoad() != null && !props.getDocumentsToLoad().isEmpty()) {
      result.addAll(props.getDocumentsToLoad());
      return result;
    }

    // 2) Ellers: les base-dir fra props, evt. fallback til @Value fra YAML
    String baseDir = props.getDocumentsToLoadDir();

    if ((baseDir == null || baseDir.isBlank()) && documentsToLoadFromYaml != null && !documentsToLoadFromYaml.isBlank()) {
      baseDir = documentsToLoadFromYaml;
    }

    if (baseDir == null || baseDir.isBlank()) {
      log.warn("Ingen documentsToLoad(base-dir) angitt. Sett f.eks. 'documentsToLoad: classpath:/tmp/docs/' i YAML.");
      return result;
    }

    // Normaliser – sørg for at det slutter med '/'
    if (!baseDir.endsWith("/")) {
      baseDir = baseDir + "/";
    }

    // Bruk PathMatchingResourcePatternResolver for wildcard
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    // Søker rekursivt i undermapper: **/*.ext
    List<String> exts = Arrays.asList("pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");
    for (String ext : exts) {
      String pattern = baseDir + "**/*." + ext; // f.eks. classpath:/tmp/docs/**/*.(pdf/docx/…)
      try {
        Resource[] found = resolver.getResources(pattern);
        result.addAll(Arrays.asList(found));
        if (found.length > 0) {
          log.debug("Fant {} filer med endelse .{}", found.length, ext);
        }
      } catch (Exception e) {
        log.warn("Kunne ikke søke etter filer med endelse .{}: {}", ext, e.getMessage());
      }
    }

    if (result.isEmpty()) {
      log.warn("Fant ingen filer i '{}' med endelser {}", baseDir, extsToString());
    } else {
      if (log.isInfoEnabled()) {
        log.info("Filer som lastes fra '{}':", baseDir);
        result.forEach(r -> log.info(" - {}", safeName(r)));
      }
    }

    return result;
  }

  private static String safeName(Resource r) {
    try { return r.getURL().toString(); }
    catch (Exception e) { return r.getFilename(); }
  }

  private static String extsToString() {
    return "[pdf, docx, doc, txt, md, png, jpg, jpeg, gif, bmp, tiff, webp, svg]";
  }

  /**
   * Prosesserer dokumenter og legger til metadata for innholdstype.
   * Håndterer både tekst og bildeinnhold med riktig metadata.
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
        
        // Sjekk om det er et bilde
        if (lowerFilename.endsWith(".png") || lowerFilename.endsWith(".jpg") || 
            lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".gif") || 
            lowerFilename.endsWith(".bmp") || lowerFilename.endsWith(".tiff") || 
            lowerFilename.endsWith(".webp") || lowerFilename.endsWith(".svg")) {
          
          // For bilder: legg til metadata som indikerer at det er et bilde
          Document imageDoc = new Document(doc.getText(), doc.getMetadata());
          imageDoc.getMetadata().put("content_type", "image");
          imageDoc.getMetadata().put("filename", filename);
          imageDoc.getMetadata().put("source", safeName(resource));
          processedDocs.add(imageDoc);
          
        } else {
          // For tekstdokumenter: legg til metadata som indikerer at det er tekst
          Document textDoc = new Document(doc.getText(), doc.getMetadata());
          textDoc.getMetadata().put("content_type", "text");
          textDoc.getMetadata().put("filename", filename);
          textDoc.getMetadata().put("source", safeName(resource));
          processedDocs.add(textDoc);
        }
        
      } catch (Exception e) {
        log.warn("Feil ved prosessering av dokument fra '{}': {}", safeName(resource), e.getMessage());
        processedDocs.add(doc); // Fallback til originalt dokument
      }
    }
    
    return processedDocs;
  }

  /**
   * Oppretter CryptoService fra konfigurasjon.
   */
  private CryptoService createCryptoService(VectorStoreProperties props) {
    try {
      String keyBase64 = props.getEncryptionKeyBase64();
      if (keyBase64 == null || keyBase64.isBlank()) {
        // Prøv miljøvariabel som fallback
        keyBase64 = System.getenv("VECTORSTORE_ENC_KEY");
      }
      
      if (keyBase64 == null || keyBase64.isBlank()) {
        log.warn("Ingen krypteringsnøkkel funnet i konfigurasjon eller miljøvariabel VECTORSTORE_ENC_KEY");
        return null;
      }
      
      byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
      return new CryptoService(keyBytes);
    } catch (Exception e) {
      log.error("Feil ved oppretting av CryptoService: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Krypterer dokumenter og legger til metadata.
   */
  private List<Document> encryptDocuments(List<Document> documents, CryptoService crypto) {
    return documents.stream()
        .map(doc -> {
          try {
            String originalText = doc.getText();
            if (originalText == null || originalText.trim().isEmpty()) {
              return doc; // Ikke krypter tom tekst
            }
            
            CryptoService.EncResult encrypted = crypto.encrypt(originalText);
            
            // Opprett nytt dokument med kryptert tekst og metadata
            Document encryptedDoc = new Document(encrypted.cipherBase64(), doc.getMetadata());
            encryptedDoc.getMetadata().put("enc", "aesgcm");
            encryptedDoc.getMetadata().put("enc_iv", encrypted.ivBase64());
            
            return encryptedDoc;
          } catch (Exception e) {
            log.error("Feil ved kryptering av dokument: {}", e.getMessage(), e);
            return doc; // Returner originalt dokument ved feil
          }
        })
        .toList();
  }
}