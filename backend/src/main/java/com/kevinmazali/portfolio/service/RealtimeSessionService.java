package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Proxies browser SDP to OpenAI {@code /v1/realtime/calls} with a server-built session config.
 */
@Slf4j
@Service
public class RealtimeSessionService {

  private static final String OPENAI_REALTIME_CALLS = "https://api.openai.com/v1/realtime/calls";

  private final RealtimeProperties realtimeProperties;
  private final AiBudgetService aiBudgetService;
  private final AiBudgetProperties budgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker;
  private final String openAiApiKey;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private String instructionsEn;
  private String instructionsNo;

  public RealtimeSessionService(
      RealtimeProperties realtimeProperties,
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.openAiRealtimeHttpInvoker = openAiRealtimeHttpInvoker;
    this.openAiApiKey = openAiApiKey;
  }

  /** Lazy-load instruction files from classpath. */
  private String instructionsForLanguage(String lang) {
    if ("no".equalsIgnoreCase(lang)) {
      if (instructionsNo == null) {
        instructionsNo = loadPromptFile("prompts/realtime-voice-no.txt");
      }
      return instructionsNo;
    }
    if (instructionsEn == null) {
      instructionsEn = loadPromptFile("prompts/realtime-voice-en.txt");
    }
    return instructionsEn;
  }

  private static String loadPromptFile(String path) {
    try {
      var res = new ClassPathResource(path);
      return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      return "You are a helpful assistant for Kevin's portfolio website.";
    }
  }

  /**
   * @param sdp the WebRTC SDP offer from the browser
   * @param chatLanguage optional {@code en} or {@code no} from {@code X-Chat-Language}
   * @return SDP answer text from OpenAI
   */
  public String createRealtimeCall(String sdp, String chatLanguage) {
    if (!StringUtils.hasText(openAiApiKey)) {
      throw new IllegalStateException("OpenAI API key is not configured.");
    }
    if (!StringUtils.hasText(sdp)) {
      throw new IllegalArgumentException("SDP body is required.");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    String lang = normalizeLang(chatLanguage);
    String instructions = instructionsForLanguage(lang);
    String sessionJson;
    try {
      sessionJson = buildSessionJson(instructions);
    } catch (IOException e) {
      throw new IllegalStateException("Could not build Realtime session config.", e);
    }

    String boundary = "----PortfolioBoundary" + UUID.randomUUID();
    byte[] body = buildMultipartBody(boundary, sdp, sessionJson);

    String safetyId = budgetUserId.length() > 128 ? budgetUserId.substring(0, 128) : budgetUserId;

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_REALTIME_CALLS))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + openAiApiKey)
            .header("OpenAI-Safety-Identifier", safetyId)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

    try {
      HttpResponse<String> response = openAiRealtimeHttpInvoker.invoke(request);
      int status = response.statusCode();
      if (status >= 200 && status < 300) {
        aiBudgetService.recordUsage(
            budgetUserId,
            realtimeProperties.getModel(),
            realtimeProperties.getReservationInputTokens(),
            realtimeProperties.getReservationOutputTokens(),
            anonymous,
            null,
            "realtime_voice_session");
        return response.body();
      }
      log.warn("OpenAI realtime/calls failed: status={} body={}", status, truncate(response.body(), 2000));
      throw new IllegalStateException("OpenAI Realtime session failed (HTTP " + status + ").");
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalStateException("Could not reach OpenAI Realtime API: " + e.getMessage(), e);
    }
  }

  private static String normalizeLang(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "en";
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("no".equals(v) || "nb".equals(v) || "nn".equals(v)) {
      return "no";
    }
    return "en";
  }

  private String buildSessionJson(String instructions) throws IOException {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "realtime");
    root.put("model", realtimeProperties.getModel());
    root.put("instructions", instructions);
    root.put("reasoning_effort", realtimeProperties.getReasoningEffort());
    root.put("max_response_output_tokens", realtimeProperties.getMaxResponseOutputTokens());
    root.putArray("modalities").add("text").add("audio");

    ObjectNode audio = objectMapper.createObjectNode();
    ObjectNode output = objectMapper.createObjectNode();
    output.put("voice", realtimeProperties.getVoice());
    audio.set("output", output);
    root.set("audio", audio);

    ObjectNode inputTx = objectMapper.createObjectNode();
    inputTx.put("model", "whisper-1");
    root.set("input_audio_transcription", inputTx);

    return objectMapper.writeValueAsString(root);
  }

  private static byte[] buildMultipartBody(String boundary, String sdp, String sessionJson) {
    String sep = "--" + boundary + "\r\n";
    StringBuilder sb = new StringBuilder();
    sb.append(sep);
    sb.append("Content-Disposition: form-data; name=\"sdp\"\r\n");
    sb.append("Content-Type: application/sdp\r\n\r\n");
    sb.append(sdp);
    if (!sdp.endsWith("\n")) {
      sb.append("\r\n");
    }
    sb.append(sep);
    sb.append("Content-Disposition: form-data; name=\"session\"\r\n");
    sb.append("Content-Type: application/json\r\n\r\n");
    sb.append(sessionJson);
    sb.append("\r\n");
    sb.append("--").append(boundary).append("--\r\n");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max) + "...";
  }
}
