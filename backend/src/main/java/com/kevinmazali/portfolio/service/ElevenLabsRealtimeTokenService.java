package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.analytics.RealtimeVoiceAnalyticsContext;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mints browser-safe ElevenLabs Conversational AI WebRTC tokens for configured agents.
 */
@Service
public class ElevenLabsRealtimeTokenService {

  private static final Logger log = LoggerFactory.getLogger(ElevenLabsRealtimeTokenService.class);

  private static final String ELEVENLABS_TOKEN_URL =
      "https://api.elevenlabs.io/v1/convai/conversation/token";

  private final RealtimeProperties realtimeProperties;
  private final RealtimeModelCatalog realtimeModelCatalog;
  private final AiBudgetProperties budgetProperties;
  private final AiBudgetService aiBudgetService;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker;
  @Nullable private final PostHogLlmService postHogLlmService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ElevenLabsRealtimeTokenService(
      RealtimeProperties realtimeProperties,
      RealtimeModelCatalog realtimeModelCatalog,
      AiBudgetProperties budgetProperties,
      AiBudgetService aiBudgetService,
      AiCircuitBreaker aiCircuitBreaker,
      ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker,
      @Autowired(required = false) @Nullable PostHogLlmService postHogLlmService) {
    this.realtimeProperties = realtimeProperties;
    this.realtimeModelCatalog = realtimeModelCatalog;
    this.budgetProperties = budgetProperties;
    this.aiBudgetService = aiBudgetService;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.elevenLabsRealtimeHttpInvoker = elevenLabsRealtimeHttpInvoker;
    this.postHogLlmService = postHogLlmService;
  }

  public String createConversationToken(String modelId) {
    return createConversationToken(modelId, null);
  }

  public String createConversationToken(String modelId, @Nullable RealtimeVoiceAnalyticsContext voiceAnalytics) {
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

    URI tokenUri = buildTokenUri(agent);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(tokenUri)
        .timeout(Duration.ofSeconds(30))
        .header("xi-api-key", apiKey)
        .GET()
        .build();

    log.info(
        "ElevenLabs token request starting: requested_model_id={} agent_id={} environment={} branch_id={} trace_id={}",
        safe(modelId),
        safe(agent.getAgentId()),
        describeEnvironment(agent),
        describeBranch(agent),
        voiceAnalytics != null ? safe(voiceAnalytics.traceId()) : "n/a");
    log.debug("ElevenLabs token request: GET {}", tokenUri);

    long spanStartNs = System.nanoTime();
    try {
      HttpResponse<String> response = elevenLabsRealtimeHttpInvoker.invoke(request);
      int status = response.statusCode();
      log.debug("ElevenLabs token response: status={}", status);
      if (status >= 200 && status < 300) {
        String token = parseToken(response.body());
        if (!StringUtils.hasText(token)) {
          captureElevenLabsTokenSpan(
              voiceAnalytics, budgetUserId, anonymous, spanStartNs, true);
          throw new RealtimeSessionException(
              HttpStatus.BAD_GATEWAY,
              RealtimeErrorCode.ELEVENLABS_REJECTED,
              "ElevenLabs did not return a conversation token.");
        }
        TokenDiagnostics tokenDiagnostics = decodeTokenDiagnostics(token);
        logTokenIssued(agent, tokenDiagnostics, voiceAnalytics);
        captureElevenLabsTokenSpan(
            voiceAnalytics, budgetUserId, anonymous, spanStartNs, false);
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
      captureElevenLabsTokenSpan(
          voiceAnalytics, budgetUserId, anonymous, spanStartNs, true);
      RealtimeErrorCode code =
          status >= 500 ? RealtimeErrorCode.ELEVENLABS_SERVER_ERROR : RealtimeErrorCode.ELEVENLABS_REJECTED;
      String detail = summarizeErrorBody(response.body());
      log.warn(
          "ElevenLabs token request rejected: status={} code={} agent_id={} environment={} branch_id={} detail={} trace_id={}",
          status,
          code,
          safe(agent.getAgentId()),
          describeEnvironment(agent),
          describeBranch(agent),
          StringUtils.hasText(detail) ? detail : "n/a",
          voiceAnalytics != null ? safe(voiceAnalytics.traceId()) : "n/a");
      String message = StringUtils.hasText(detail)
          ? "ElevenLabs rejected the session: " + detail
          : "ElevenLabs session token failed (HTTP " + status + ").";
      throw new RealtimeSessionException(HttpStatus.BAD_GATEWAY, code, message);
    } catch (IOException | InterruptedException e) {
      captureElevenLabsTokenSpan(
          voiceAnalytics, budgetUserId, anonymous, spanStartNs, true);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.warn(
          "ElevenLabs token request failed: agent_id={} environment={} branch_id={} exception={} trace_id={}",
          safe(agent.getAgentId()),
          describeEnvironment(agent),
          describeBranch(agent),
          e.toString(),
          voiceAnalytics != null ? safe(voiceAnalytics.traceId()) : "n/a");
      throw new RealtimeSessionException(
          HttpStatus.BAD_GATEWAY,
          RealtimeErrorCode.ELEVENLABS_UNREACHABLE,
          "Could not reach ElevenLabs API: " + e.getMessage(),
          e);
    }
  }

  private void captureElevenLabsTokenSpan(
      @Nullable RealtimeVoiceAnalyticsContext ctx,
      String distinctId,
      boolean anonymous,
      long startNanos,
      boolean error) {
    PostHogLlmService ph = postHogLlmService;
    if (ph == null || !ph.isEnabled() || ctx == null) {
      return;
    }
    double latencySec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
    String spanId = UUID.randomUUID().toString();
    ph.captureSpanAsync(
        distinctId,
        ctx.traceId(),
        ctx.sessionId(),
        spanId,
        ctx.traceId(),
        "realtime_elevenlabs_token",
        latencySec,
        error,
        anonymous);
  }

  private void logTokenIssued(
      RealtimeProperties.ElevenLabsAgent agent,
      TokenDiagnostics tokenDiagnostics,
      @Nullable RealtimeVoiceAnalyticsContext voiceAnalytics) {
    log.info(
        "ElevenLabs token issued: agent_id={} environment={} branch_id={} issuer={} room={} subject={} participant_name={} expires_at={} trace_id={}",
        safe(agent.getAgentId()),
        describeEnvironment(agent),
        describeBranch(agent),
        tokenDiagnostics.issuer(),
        tokenDiagnostics.room(),
        tokenDiagnostics.subject(),
        tokenDiagnostics.participantName(),
        tokenDiagnostics.expiresAt(),
        voiceAnalytics != null ? safe(voiceAnalytics.traceId()) : "n/a");
    if (!log.isDebugEnabled()) {
      return;
    }
    log.debug(
        "ElevenLabs token diagnostics: not_before={} raw_exp={} raw_nbf={}",
        tokenDiagnostics.notBefore(),
        tokenDiagnostics.rawExp(),
        tokenDiagnostics.rawNbf());
  }

  /**
   * Decode the JWT payload to log embedded LiveKit routing data without exposing the token itself.
   */
  private TokenDiagnostics decodeTokenDiagnostics(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        return TokenDiagnostics.unknown();
      }
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      JsonNode claims = objectMapper.readTree(payload);
      return new TokenDiagnostics(
          safe(claims.path("iss").asText("")),
          safe(claims.path("sub").asText("")),
          safe(claims.path("name").asText("")),
          safe(claims.path("video").path("room").asText("")),
          formatUnixTimestamp(claims.path("exp")),
          formatUnixTimestamp(claims.path("nbf")),
          claims.path("exp").asLong(0),
          claims.path("nbf").asLong(0));
    } catch (Exception e) {
      log.debug("ElevenLabs token: could not decode JWT claims: {}", e.getMessage());
      return TokenDiagnostics.unknown();
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
      String summarized = summarizeJsonError(root);
      if (StringUtils.hasText(summarized)) {
        return truncateErrorSummary(summarized);
      }
      return truncateErrorSummary(trimmed);
    } catch (Exception e) {
      return truncateErrorSummary(trimmed);
    }
  }

  private static String truncateErrorSummary(String value) {
    return value.length() > 500 ? value.substring(0, 500) + "..." : value;
  }

  /**
   * ElevenLabs and shared infrastructure often return structured errors: {@code detail} may be a
   * string, array, or object, and {@code validation_details} may list field-level issues.
   */
  private String summarizeJsonError(JsonNode root) {
    String fromDetail = summarizeDetailNode(root.path("detail"));
    if (StringUtils.hasText(fromDetail)) {
      return fromDetail;
    }
    String fromValidation = summarizeValidationDetailsArray(root.path("validation_details"));
    if (StringUtils.hasText(fromValidation)) {
      return fromValidation;
    }
    String message = root.path("message").asText("");
    if (StringUtils.hasText(message)) {
      return message;
    }
    return "";
  }

  private String summarizeDetailNode(JsonNode detail) {
    if (detail.isMissingNode() || detail.isNull()) {
      return "";
    }
    if (detail.isTextual()) {
      return detail.asText("");
    }
    if (detail.isArray()) {
      return summarizeValidationLikeArray(detail);
    }
    if (detail.isObject()) {
      String innerMessage = detail.path("message").asText("");
      if (StringUtils.hasText(innerMessage)) {
        return innerMessage;
      }
      String fromNested = summarizeValidationDetailsArray(detail.path("validation_details"));
      if (StringUtils.hasText(fromNested)) {
        return fromNested;
      }
    }
    return "";
  }

  private String summarizeValidationDetailsArray(JsonNode node) {
    if (!node.isArray() || node.isEmpty()) {
      return "";
    }
    return summarizeValidationLikeArray(node);
  }

  private String summarizeValidationLikeArray(JsonNode arr) {
    StringBuilder sb = new StringBuilder();
    for (JsonNode item : arr) {
      String part = summarizeValidationEntry(item);
      if (StringUtils.hasText(part)) {
        if (sb.length() > 0) {
          sb.append("; ");
        }
        sb.append(part);
      }
    }
    return sb.toString();
  }

  private String summarizeValidationEntry(JsonNode item) {
    if (item.isTextual()) {
      return item.asText("");
    }
    if (!item.isObject()) {
      return "";
    }
    String msg = firstNonBlankText(
        item,
        "msg",
        "message",
        "detail",
        "description",
        "error",
        "reason",
        "issue");
    if (StringUtils.hasText(msg)) {
      return msg;
    }
    JsonNode loc = item.path("loc");
    String locStr = joinLocSegments(loc);
    try {
      String compact = objectMapper.writeValueAsString(item);
      return StringUtils.hasText(locStr) ? locStr + ": " + compact : compact;
    } catch (JsonProcessingException e) {
      return locStr;
    }
  }

  private static String firstNonBlankText(JsonNode object, String... fieldNames) {
    for (String name : fieldNames) {
      String v = object.path(name).asText("");
      if (StringUtils.hasText(v)) {
        return v;
      }
    }
    return "";
  }

  private static String joinLocSegments(JsonNode loc) {
    if (!loc.isArray() || loc.isEmpty()) {
      return "";
    }
    StringBuilder lb = new StringBuilder();
    for (JsonNode segment : loc) {
      if (lb.length() > 0) {
        lb.append('.');
      }
      lb.append(segment.asText(""));
    }
    return lb.toString();
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String describeEnvironment(RealtimeProperties.ElevenLabsAgent agent) {
    return StringUtils.hasText(agent.getEnvironment()) ? agent.getEnvironment().trim() : "production";
  }

  private static String describeBranch(RealtimeProperties.ElevenLabsAgent agent) {
    return StringUtils.hasText(agent.getBranchId()) ? agent.getBranchId().trim() : "default";
  }

  private static String safe(@Nullable String value) {
    return StringUtils.hasText(value) ? value.trim() : "n/a";
  }

  private static String formatUnixTimestamp(JsonNode value) {
    long raw = value.asLong(0);
    if (raw <= 0) {
      return "n/a";
    }
    try {
      return Instant.ofEpochSecond(raw).toString();
    } catch (Exception e) {
      return Long.toString(raw);
    }
  }

  private record TokenDiagnostics(
      String issuer,
      String subject,
      String participantName,
      String room,
      String expiresAt,
      String notBefore,
      long rawExp,
      long rawNbf) {

    private static TokenDiagnostics unknown() {
      return new TokenDiagnostics("n/a", "n/a", "n/a", "n/a", "n/a", "n/a", 0, 0);
    }
  }
}
