package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mints browser-safe ElevenLabs Conversational AI WebRTC tokens for configured agents.
 */
@Service
public class ElevenLabsRealtimeTokenService {

  private static final String ELEVENLABS_TOKEN_URL =
      "https://api.elevenlabs.io/v1/convai/conversation/token";

  private final RealtimeProperties realtimeProperties;
  private final RealtimeModelCatalog realtimeModelCatalog;
  private final AiBudgetProperties budgetProperties;
  private final AiBudgetService aiBudgetService;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ElevenLabsRealtimeTokenService(
      RealtimeProperties realtimeProperties,
      RealtimeModelCatalog realtimeModelCatalog,
      AiBudgetProperties budgetProperties,
      AiBudgetService aiBudgetService,
      AiCircuitBreaker aiCircuitBreaker,
      ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker) {
    this.realtimeProperties = realtimeProperties;
    this.realtimeModelCatalog = realtimeModelCatalog;
    this.budgetProperties = budgetProperties;
    this.aiBudgetService = aiBudgetService;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.elevenLabsRealtimeHttpInvoker = elevenLabsRealtimeHttpInvoker;
  }

  public String createConversationToken(String modelId) {
    var agent = realtimeModelCatalog.findElevenLabsAgent(modelId);
    if (agent == null) {
      throw new RealtimeSessionException(
          HttpStatus.BAD_REQUEST,
          RealtimeErrorCode.VOICE_MODEL_NOT_CONFIGURED,
          "ElevenLabs voice model is not configured.");
    }

    String apiKey = realtimeProperties.getProviders().getElevenlabs().getApiKey();
    if (!StringUtils.hasText(apiKey)) {
      throw new RealtimeSessionException(
          HttpStatus.SERVICE_UNAVAILABLE,
          RealtimeErrorCode.API_KEY_MISSING,
          "ElevenLabs API key is not configured.");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(buildTokenUri(agent))
        .timeout(Duration.ofSeconds(30))
        .header("xi-api-key", apiKey)
        .GET()
        .build();

    try {
      HttpResponse<String> response = elevenLabsRealtimeHttpInvoker.invoke(request);
      int status = response.statusCode();
      if (status >= 200 && status < 300) {
        String token = parseToken(response.body());
        if (!StringUtils.hasText(token)) {
          throw new RealtimeSessionException(
              HttpStatus.BAD_GATEWAY,
              RealtimeErrorCode.ELEVENLABS_REJECTED,
              "ElevenLabs did not return a conversation token.");
        }
        aiBudgetService.recordUsage(
            budgetUserId,
            "elevenlabs:" + agent.getAgentId().trim(),
            0,
            0,
            anonymous,
            null,
            "elevenlabs_voice_session");
        return token;
      }
      RealtimeErrorCode code =
          status >= 500 ? RealtimeErrorCode.ELEVENLABS_SERVER_ERROR : RealtimeErrorCode.ELEVENLABS_REJECTED;
      String detail = summarizeErrorBody(response.body());
      String message = StringUtils.hasText(detail)
          ? "ElevenLabs rejected the session: " + detail
          : "ElevenLabs session token failed (HTTP " + status + ").";
      throw new RealtimeSessionException(HttpStatus.BAD_GATEWAY, code, message);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RealtimeSessionException(
          HttpStatus.BAD_GATEWAY,
          RealtimeErrorCode.ELEVENLABS_UNREACHABLE,
          "Could not reach ElevenLabs API: " + e.getMessage(),
          e);
    }
  }

  private URI buildTokenUri(RealtimeProperties.ElevenLabsAgent agent) {
    StringBuilder url = new StringBuilder(ELEVENLABS_TOKEN_URL)
        .append("?agent_id=")
        .append(urlEncode(agent.getAgentId().trim()));
    if (StringUtils.hasText(agent.getEnvironment())) {
      url.append("&environment=").append(urlEncode(agent.getEnvironment().trim()));
    }
    if (StringUtils.hasText(agent.getBranchId())) {
      url.append("&branch_id=").append(urlEncode(agent.getBranchId().trim()));
    }
    return URI.create(url.toString());
  }

  private String parseToken(String body) throws IOException {
    if (!StringUtils.hasText(body)) {
      return "";
    }
    JsonNode root = objectMapper.readTree(body);
    return root.path("token").asText("");
  }

  private String summarizeErrorBody(String body) {
    if (!StringUtils.hasText(body)) {
      return "";
    }
    String trimmed = body.trim();
    try {
      JsonNode root = objectMapper.readTree(trimmed);
      String detail = root.path("detail").asText("");
      if (StringUtils.hasText(detail)) {
        return detail;
      }
      String message = root.path("message").asText("");
      if (StringUtils.hasText(message)) {
        return message;
      }
      return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    } catch (Exception e) {
      return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
