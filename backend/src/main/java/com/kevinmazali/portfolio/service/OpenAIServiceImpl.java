package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link OpenAIService} that performs RAG:
 * - expands the query to multiple languages,
 * - retrieves similar documents from the vector store,
 * - builds a prompt and invokes the chat model.
 */
@Service
public class OpenAIServiceImpl implements OpenAIService {

  private final ChatModel chatModel;
  private final VectorStore vectorStore;

  public OpenAIServiceImpl(ChatModel chatModel, @Lazy VectorStore vectorStore) {
    this.chatModel = chatModel;
    this.vectorStore = vectorStore;
  }

  /**
   * Executes a Retrieval-Augmented Generation flow:
   * 1) expand the query to English and Norwegian,
   * 2) retrieve and de-duplicate the most similar documents,
   * 3) compose the prompt and call the chat model.
   *
   * @param question the user question
   * @return the generated {@link Answer}
   */
  @Override
  public Answer getAnswer(Question question) {
    // 1) Expand the query: original + translated to EN and NO
    List<String> queries = expandQueryToLanguages(question.question());

    // 2) Fetch top documents for each variant and merge
    List<Document> documents = queries.stream()
        .flatMap(q -> vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(40)
                .build()
        ).stream())
        // Deduplicate on text content to avoid duplicates across query variants
        .distinct()
        .limit(40)
        .toList();

    List<String> contentList = documents.stream().map(Document::getText).toList();

    // 3) Read prompt template from classpath (also works when packaged as a JAR)
    String ragPromptTemplate = loadPromptTemplateFromClasspath("templates/rag-prompt-template.st");

    PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
    Prompt prompt = promptTemplate.create(Map.of(
        "input", question.question(),
        "documents", String.join("\n", contentList)
    ));

    // 4) Call the model. Max token limit is set via application.yaml
    ChatResponse response = chatModel.call(prompt);
    return new Answer(response.getResult().getOutput().getText());
  }

  /**
   * Creates query variants in the original language, English, and Norwegian.
   * Falls back to the original only upon errors.
   */
  private List<String> expandQueryToLanguages(String original) {
    try {
      // Simple prompt for quick translation without explanations
      String sys = """
      Translate the user query into both English and Norwegian.
      Return ONLY this exact JSON object with double quotes and no extra text:
      {"en": "<english>", "no": "<norwegian>"}
      """.strip();

      Prompt p = new PromptTemplate("{sys}\nUser: {q}")
          .create(Map.of("sys", sys, "q", original));

      ChatResponse r = chatModel.call(p);
      String json = r.getResult().getOutput().getText();

      // Very simple parsing to avoid extra dependencies
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
   * Extracts a simple string value from a flat JSON object without using a parser.
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

  /**
   * Loads a prompt template from the classpath; works when packaged as a JAR as well.
   *
   * @param resourceName the classpath resource name (e.g. "templates/rag-prompt-template.st")
   * @return the template text
   */
  private String loadPromptTemplateFromClasspath(String resourceName) {
    try {
      ClassPathResource res = new ClassPathResource(resourceName);
      try (InputStream in = res.getInputStream()) {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not read " + resourceName + " from classpath", e);
    }
  }
}
