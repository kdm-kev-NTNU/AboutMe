package com.kevinmazali.portfolio.config;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
      VectorStoreProperties vectorStoreProperties
  ) throws IOException {

    // Bygg store med påkrevd EmbeddingModel (din versjon krever dette)
    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

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
        List<Document> splitDocs = textSplitter.apply(docs);
        store.add(splitDocs);
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
    List<String> exts = Arrays.asList("pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg");
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
    return "[pdf, docx, doc, txt, md, png, jpg, jpeg]";
  }
}