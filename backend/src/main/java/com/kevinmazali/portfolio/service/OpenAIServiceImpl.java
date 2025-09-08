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
    // 1) Utvid spørringen: original + oversatt til EN og NO
    List<String> queries = expandQueryToLanguages(question.question());

    // 2) Hent top dokumenter for hver variant og slå sammen
    List<Document> documents = queries.stream()
        .flatMap(q -> vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(40)
                .build()
        ).stream())
        // Dedup på tekstinnhold for å unngå duplikater fra flere spørringer
        .distinct()
        .limit(40)
        .toList();

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

  /**
   * Lager spørringsvarianter på originalspråk + engelsk + norsk.
   * Faller tilbake til kun original ved feil.
   */
  private List<String> expandQueryToLanguages(String original) {
    try {
      // Enkel prompt for rask oversettelse uten forklaringer
      String sys = """
      Translate the user query into both English and Norwegian.
      Return ONLY this exact JSON object with double quotes and no extra text:
      {"en": "<english>", "no": "<norwegian>"}
      """.strip();

      Prompt p = new PromptTemplate("{sys}\nUser: {q}")
          .create(Map.of("sys", sys, "q", original));

      ChatResponse r = chatModel.call(p);
      String json = r.getResult().getOutput().getText();

      // Svært enkel parsing for å unngå ekstra avhengigheter
      String en = extractJsonValue(json, "en");
      String no = extractJsonValue(json, "no");

      return List.of(original,
          en == null || en.isBlank() ? original : en,
          no == null || no.isBlank() ? original : no);
    } catch (Exception e) {
      return List.of(original);
    }
  }

  /**
   * Ekstraherer en enkel strengverdi fra et flatt JSON-objekt uten å dra inn en parser.
   */
  private String extractJsonValue(String json, String key) {
    try {
      String marker = "\"" + key + "\"" + ":";
      int i = json.indexOf(marker);
      if (i < 0) return null;
      int start = json.indexOf('"', i + marker.length());
      if (start < 0) return null;
      int end = json.indexOf('"', start + 1);
      if (end < 0) return null;
      return json.substring(start + 1, end);
    } catch (Exception ex) {
      return null;
    }
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
