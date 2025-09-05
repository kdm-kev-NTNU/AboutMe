package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.crypto.CryptoService;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

  private final ChatModel chatModel;
  private final SimpleVectorStore vectorStore;

  @Override
  public Answer getAnswer(Question question) {
    // 1) Hent top-N dokumenter (bruk konstruktør-varianten for SearchRequest)
    List<Document> documents = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(question.question())
            .topK(4)
            .build()
    );

    // 2) Dekrypter innhold ved behov
    CryptoService crypto = cryptoFromEnv();
    List<String> contentList = documents.stream()
        .map(d -> {
          Object enc = d.getMetadata().get("enc");
          if ("aesgcm".equals(enc) && crypto != null) {
            String iv = String.valueOf(d.getMetadata().get("enc_iv"));
            String ct = d.getText();
            try {
              return crypto.decrypt(iv, ct);
            } catch (RuntimeException ex) {
              Object src = d.getMetadata().getOrDefault("source", "(ukjent kilde)");
              return "[Kunne ikke dekryptere chunk – kilde: " + src + "]";
            }
          } else {
            return d.getText();
          }
        })
        .toList();

    // 3) Les prompt-template fra classpath (fungerer også når pakket som JAR)
    String ragPromptTemplate = loadPromptTemplateFromClasspath("templates/rag-prompt-template.st");

    PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
    Prompt prompt = promptTemplate.create(Map.of(
        "input", question.question(),
        "documents", String.join("\n", contentList)
    ));

    // 4) Kall modellen
    ChatResponse response = chatModel.call(prompt);
    return new Answer(response.getResult().getOutput().getText());
  }

  private CryptoService cryptoFromEnv() {
    String b64 = System.getenv("VECTORSTORE_ENC_KEY");
    if (b64 == null || b64.isBlank()) return null;
    byte[] key = Base64.getDecoder().decode(b64);
    try {
      return new CryptoService(key);
    } catch (IllegalArgumentException e) {
      return null; // feil lengde -> deaktiver dekryptering
    }
  }

  private String loadPromptTemplateFromClasspath(String resourceName) {
    try {
      ClassPathResource res = new ClassPathResource(resourceName);
      try (InputStream in = res.getInputStream()) {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      throw new RuntimeException("Kunne ikke lese " + resourceName + " fra classpath", e);
    }
  }
}
