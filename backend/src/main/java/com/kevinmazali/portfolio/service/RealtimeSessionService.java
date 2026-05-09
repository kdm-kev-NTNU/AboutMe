package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
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
import org.springframework.http.HttpStatus;
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
  private final RealtimeProfileService realtimeProfileService;
  private final String openAiApiKey;

  // Spring Boot 4 auto-configures the new tools.jackson.databind.ObjectMapper, so injecting the
  // legacy com.fasterxml.jackson.databind.ObjectMapper would fail bean lookup. ObjectMapper is
  // thread-safe; one private instance per service is fine.
  private final ObjectMapper objectMapper = new ObjectMapper();

  private String instructionsEn;
  private String instructionsNo;

  public RealtimeSessionService(
      RealtimeProperties realtimeProperties,
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker,
      RealtimeProfileService realtimeProfileService,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.openAiRealtimeHttpInvoker = openAiRealtimeHttpInvoker;
    this.realtimeProfileService = realtimeProfileService;
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

  private String loadPromptFile(String path) {
    try {
      var res = new ClassPathResource(path);
      return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      log.warn("Could not load realtime prompt {}: {}", path, e.getMessage());
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
      throw new RealtimeSessionException(
          HttpStatus.SERVICE_UNAVAILABLE,
          RealtimeErrorCode.API_KEY_MISSING,
          "OpenAI API key is not configured.");
    }
    if (!StringUtils.hasText(sdp)) {
      throw new IllegalArgumentException("SDP body is required.");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    String lang = normalizeLang(chatLanguage);
    String instructions = instructionsForLanguage(lang)
        .replace("{profile_card}", realtimeProfileService.profileCard(lang));
    String sessionJson;
    try {
      sessionJson = buildSessionJson(instructions);
    } catch (IOException e) {
      log.warn(
          "realtime_session_config_failed budgetUserId={} message={}",
          truncateId(budgetUserId),
          e.getMessage());
      throw new RealtimeSessionException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          RealtimeErrorCode.SESSION_CONFIG_FAILED,
          "Could not build Realtime session config.",
          e);
    }

    String boundary = "----PortfolioBoundary" + UUID.randomUUID();
    byte[] body = buildMultipartBody(boundary, sdp, sessionJson);

    String safetyId = AiRequestContext.openAiSafetyIdentifier(budgetUserId);

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
      String responseBody = response.body();
      String openAiSummary = summarizeOpenAiErrorBody(responseBody);
      RealtimeErrorCode code =
          status >= 500 ? RealtimeErrorCode.OPENAI_SERVER_ERROR : RealtimeErrorCode.OPENAI_REJECTED;
      if (code == RealtimeErrorCode.OPENAI_REJECTED) {
        log.warn(
            "openai_realtime_sdp_rejected sdpChars={} sdpFirstLine={}",
            sdp.length(),
            sdpFirstLineForLog(sdp));
      }
      log.warn(
          "openai_realtime_calls_failed budgetUserId={} httpStatus={} errorCode={} openAiDetail={} bodyTrunc={}",
          truncateId(budgetUserId),
          status,
          code,
          openAiSummary,
          truncate(responseBody, 2000));
      String userMessage =
          StringUtils.hasText(openAiSummary)
              ? "OpenAI rejected the session: " + openAiSummary
              : "OpenAI Realtime session failed (HTTP " + status + ").";
      throw new RealtimeSessionException(HttpStatus.BAD_GATEWAY, code, userMessage);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.warn(
          "openai_realtime_unreachable budgetUserId={} errorCode={} message={}",
          truncateId(budgetUserId),
          RealtimeErrorCode.OPENAI_UNREACHABLE,
          e.getMessage());
      throw new RealtimeSessionException(
          HttpStatus.BAD_GATEWAY,
          RealtimeErrorCode.OPENAI_UNREACHABLE,
          "Could not reach OpenAI Realtime API: " + e.getMessage(),
          e);
    }
  }

  /** Best-effort summary of OpenAI JSON error body for logs and user-facing text. */
  String summarizeOpenAiErrorBody(String body) {
    if (!StringUtils.hasText(body)) {
      return "";
    }
    String trimmed = body.trim();
    try {
      JsonNode root = objectMapper.readTree(trimmed);
      JsonNode err = root.path("error");
      if (err.isMissingNode() || err.isNull()) {
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
      }
      if (err.isTextual()) {
        return err.asText();
      }
      String msg = err.path("message").asText("");
      String code = err.path("code").asText("");
      if (StringUtils.hasText(msg) && StringUtils.hasText(code)) {
        return msg + " (" + code + ")";
      }
      if (StringUtils.hasText(msg)) {
        return msg;
      }
      if (StringUtils.hasText(code)) {
        return code;
      }
      return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    } catch (Exception parseEx) {
      return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }
  }

  private static String truncateId(String id) {
    if (id == null) {
      return "";
    }
    return id.length() > 64 ? id.substring(0, 64) + "..." : id;
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
    root.put("max_output_tokens", realtimeProperties.getMaxResponseOutputTokens());
    root.putArray("output_modalities").add("audio");
    addLookupTool(root);

    ObjectNode reasoning = objectMapper.createObjectNode();
    String reasoningEffort =
        StringUtils.hasText(realtimeProperties.getReasoningEffort())
            ? realtimeProperties.getReasoningEffort().trim()
            : "low";
    reasoning.put("effort", reasoningEffort);
    root.set("reasoning", reasoning);

    ObjectNode audio = objectMapper.createObjectNode();
    ObjectNode output = objectMapper.createObjectNode();
    output.put("voice", realtimeProperties.getVoice());
    audio.set("output", output);

    ObjectNode input = objectMapper.createObjectNode();
    ObjectNode transcription = objectMapper.createObjectNode();
    transcription.put("model", "whisper-1");
    input.set("transcription", transcription);

    ObjectNode turnDetection = objectMapper.createObjectNode();
    turnDetection.put("type", "semantic_vad");
    turnDetection.put("eagerness", "auto");
    turnDetection.put("create_response", true);
    turnDetection.put("interrupt_response", true);
    input.set("turn_detection", turnDetection);

    audio.set("input", input);

    root.set("audio", audio);

    return objectMapper.writeValueAsString(root);
  }

  private void addLookupTool(ObjectNode root) {
    var tools = root.putArray("tools");
    ObjectNode tool = objectMapper.createObjectNode();
    tool.put("type", "function");
    tool.put("name", "lookup_kevin_info");
    tool.put(
        "description",
        "Look up concise public facts about Kevin's studies, projects, course areas, work experience, and portfolio. Do not use for grades, private details, or questions outside Kevin's public profile.");

    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("type", "object");
    ObjectNode properties = objectMapper.createObjectNode();
    ObjectNode query = objectMapper.createObjectNode();
    query.put("type", "string");
    query.put("description", "A concise search query in the user's language.");
    properties.set("query", query);
    parameters.set("properties", properties);
    parameters.putArray("required").add("query");
    parameters.put("additionalProperties", false);
    tool.set("parameters", parameters);
    tools.add(tool);
    root.put("tool_choice", "auto");
  }

  /**
   * SDP is defined with CRLF line endings (RFC 8866). Normalizing avoids multipart parsers seeing a lone
   * {@code \n} immediately before {@code --boundary}, which can yield an empty extracted SDP on some servers.
   */
  private static String normalizeSdpLineEndings(String sdp) {
    return sdp.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\r\n");
  }

  private static String sdpFirstLineForLog(String sdp) {
    if (!StringUtils.hasText(sdp)) {
      return "";
    }
    String[] lines = sdp.split("\\R", 2);
    String first = lines[0].trim();
    return first.length() <= 120 ? first : first.substring(0, 120) + "...";
  }

  private static byte[] buildMultipartBody(String boundary, String sdp, String sessionJson) {
    String sdpNorm = normalizeSdpLineEndings(sdp);
    String sep = "--" + boundary + "\r\n";
    StringBuilder sb = new StringBuilder();
    sb.append(sep);
    sb.append("Content-Disposition: form-data; name=\"sdp\"\r\n");
    sb.append("Content-Type: application/sdp\r\n\r\n");
    sb.append(sdpNorm);
    sb.append("\r\n");
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
