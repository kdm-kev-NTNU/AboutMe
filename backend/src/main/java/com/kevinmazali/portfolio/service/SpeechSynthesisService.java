package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Turn-based text-to-speech for the standard voice mode.
 */
@Slf4j
@Service
public class SpeechSynthesisService {

  private static final String OPENAI_SPEECH_URL = "https://api.openai.com/v1/audio/speech";
  private static final int MAX_TEXT_CHARS = 1200;

  private final AiBudgetService aiBudgetService;
  private final AiBudgetProperties budgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final String openAiApiKey;
  private final String modelId;
  private final String defaultVoice;
  private final OpenAiSpeechHttpInvoker openAiSpeechHttpInvoker;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SpeechSynthesisService(
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      OpenAiSpeechHttpInvoker openAiSpeechHttpInvoker,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
      @Value("${spring.ai.openai.audio.speech.options.model:tts-1}") String modelId,
      @Value("${spring.ai.openai.audio.speech.options.voice:nova}") String defaultVoice) {
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.openAiSpeechHttpInvoker = openAiSpeechHttpInvoker;
    this.openAiApiKey = openAiApiKey;
    this.modelId = modelId;
    this.defaultVoice = defaultVoice;
  }

  public boolean isConfigured() {
    return StringUtils.hasText(openAiApiKey);
  }

  public byte[] synthesize(String text, String language) {
    if (!isConfigured()) {
      throw new IllegalStateException("OpenAI API key is not configured.");
    }
    String normalizedText = normalizeText(text);
    String voice = resolveVoice(language);

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", modelId);
    body.put("voice", voice);
    body.put("input", normalizedText);
    body.put("format", "mp3");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(OPENAI_SPEECH_URL))
        .timeout(Duration.ofSeconds(45))
        .header("Authorization", "Bearer " + openAiApiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
        .build();

    long startNs = System.nanoTime();
    try {
      HttpResponse<byte[]> response = openAiSpeechHttpInvoker.invoke(request);
      int responseStatus = response.statusCode();
      if (responseStatus < 200 || responseStatus >= 300) {
        String detail = new String(response.body(), StandardCharsets.UTF_8);
        String msg = detail.length() > 350 ? detail.substring(0, 350) + "..." : detail;
        throw new IllegalStateException("OpenAI speech API failed (HTTP " + responseStatus + "): " + msg);
      }
      double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
      aiBudgetService.recordUsage(
          budgetUserId,
          modelId,
          pseudoPromptTokensFromChars(normalizedText.length()),
          0,
          anonymous,
          latencySec,
          "audio_synthesis");
      return response.body();
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalStateException("Could not reach OpenAI speech API: " + e.getMessage(), e);
    }
  }

  static int pseudoPromptTokensFromChars(int chars) {
    return Math.max(1, chars);
  }

  private static String normalizeText(String text) {
    if (!StringUtils.hasText(text)) {
      throw new IllegalArgumentException("Text is required.");
    }
    String normalized = text.trim();
    if (normalized.length() > MAX_TEXT_CHARS) {
      throw new IllegalArgumentException("Text must be at most " + MAX_TEXT_CHARS + " characters.");
    }
    return normalized;
  }

  private String resolveVoice(String language) {
    if (!StringUtils.hasText(language)) {
      return defaultVoice;
    }
    String lang = language.trim().toLowerCase();
    if ("no".equals(lang) || "nb".equals(lang) || "nn".equals(lang)) {
      return defaultVoice;
    }
    return defaultVoice;
  }
}
