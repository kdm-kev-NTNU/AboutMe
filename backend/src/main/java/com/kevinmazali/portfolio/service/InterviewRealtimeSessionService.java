package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.InterviewProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.interview.InterviewSessionEntity;
import com.kevinmazali.portfolio.repository.InterviewSessionRepository;
import com.kevinmazali.portfolio.util.AiRequestContext;
import jakarta.annotation.PostConstruct;
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

@Slf4j
@Service
public class InterviewRealtimeSessionService {

  private static final String OPENAI_REALTIME_CALLS = "https://api.openai.com/v1/realtime/calls";

  private final RealtimeProperties realtimeProperties;
  private final InterviewProperties interviewProperties;
  private final AiBudgetService aiBudgetService;
  private final AiBudgetProperties budgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker;
  private final RealtimeModelCatalog realtimeModelCatalog;
  private final InterviewDocumentService interviewDocumentService;
  private final InterviewSessionRepository sessionRepository;
  private final String openAiApiKey;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private volatile String instructionsEn;
  private volatile String instructionsNo;

  public InterviewRealtimeSessionService(
      RealtimeProperties realtimeProperties,
      InterviewProperties interviewProperties,
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker,
      RealtimeModelCatalog realtimeModelCatalog,
      InterviewDocumentService interviewDocumentService,
      InterviewSessionRepository sessionRepository,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.interviewProperties = interviewProperties;
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.openAiRealtimeHttpInvoker = openAiRealtimeHttpInvoker;
    this.realtimeModelCatalog = realtimeModelCatalog;
    this.interviewDocumentService = interviewDocumentService;
    this.sessionRepository = sessionRepository;
    this.openAiApiKey = openAiApiKey;
  }

  @PostConstruct
  void preloadInstructionPrompts() {
    instructionsEn = loadPromptFile("prompts/interview-voice-en.txt");
    instructionsNo = loadPromptFile("prompts/interview-voice-no.txt");
  }

  public String createInterviewCall(
      String sessionId,
      String sdp,
      String chatLanguage,
      String requestedModel,
      String requestedVoice,
      String requestedReasoningEffort) {
    if (!realtimeProperties.isEnabled()) {
      throw new RealtimeSessionException(
          HttpStatus.SERVICE_UNAVAILABLE, RealtimeErrorCode.REALTIME_DISABLED, "Voice chat is disabled.");
    }
    if (!StringUtils.hasText(openAiApiKey)) {
      throw new RealtimeSessionException(
          HttpStatus.SERVICE_UNAVAILABLE, RealtimeErrorCode.API_KEY_MISSING, "OpenAI API key is not configured.");
    }
    if (!StringUtils.hasText(sdp)) {
      throw new IllegalArgumentException("SDP body is required.");
    }

    InterviewSessionEntity session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Interview session not found"));
    if (session.getDeletedAt() != null || !InterviewSessionService.STATUS_ACTIVE.equals(session.getStatus())) {
      throw new IllegalArgumentException("Interview session is not active");
    }

    String model = realtimeModelCatalog.resolveOpenAiModelId(requestedModel);
    if (!realtimeModelCatalog.isOpenAiModelConfigured(model)) {
      throw new RealtimeSessionException(
          HttpStatus.BAD_REQUEST,
          RealtimeErrorCode.VOICE_MODEL_NOT_CONFIGURED,
          "OpenAI voice model is not configured.");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = false;
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    String lang = normalizeLang(chatLanguage != null ? chatLanguage : session.getLanguage());
    String questions = interviewDocumentService.contextForSession(session.getDocumentId());
    String instructions = instructionsForLanguage(lang).replace("{questions}", questions);
    String voice = realtimeProperties.resolveVoice(
        StringUtils.hasText(requestedVoice) ? requestedVoice : session.getVoice());
    String reasoningEffort = realtimeProperties.resolveReasoningEffort(requestedReasoningEffort);
    String vadEagerness = realtimeProperties.defaultVadEagerness();

    String sessionJson;
    try {
      sessionJson = buildSessionJson(instructions, model, voice, reasoningEffort, vadEagerness);
    } catch (IOException e) {
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
            model,
            realtimeProperties.getReservationInputTokens(),
            realtimeProperties.getReservationOutputTokens(),
            anonymous,
            null,
            "interview_realtime_session");
        return response.body();
      }
      String responseBody = response.body();
      RealtimeErrorCode code =
          status >= 500 ? RealtimeErrorCode.OPENAI_SERVER_ERROR : RealtimeErrorCode.OPENAI_REJECTED;
      String userMessage =
          StringUtils.hasText(responseBody)
              ? "OpenAI rejected the session: " + truncate(responseBody, 500)
              : "OpenAI Realtime session failed (HTTP " + status + ").";
      throw new RealtimeSessionException(HttpStatus.BAD_GATEWAY, code, userMessage);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RealtimeSessionException(
          HttpStatus.BAD_GATEWAY,
          RealtimeErrorCode.OPENAI_UNREACHABLE,
          "Could not reach OpenAI Realtime API: " + e.getMessage(),
          e);
    }
  }

  private String instructionsForLanguage(String lang) {
    if ("no".equals(lang)) {
      return instructionsNo != null ? instructionsNo : loadPromptFile("prompts/interview-voice-no.txt");
    }
    return instructionsEn != null ? instructionsEn : loadPromptFile("prompts/interview-voice-en.txt");
  }

  private String loadPromptFile(String path) {
    try {
      var res = new ClassPathResource(path);
      return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      log.warn("Could not load interview prompt {}: {}", path, e.getMessage());
      return "You are an interviewer. Ask these questions in order:\n{questions}";
    }
  }

  private String buildSessionJson(
      String instructions, String model, String voice, String reasoningEffort, String vadEagerness)
      throws IOException {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "realtime");
    root.put("model", model);
    root.put("instructions", instructions);
    root.put("max_output_tokens", realtimeProperties.getMaxResponseOutputTokens());
    root.putArray("output_modalities").add("audio");

    ObjectNode reasoning = objectMapper.createObjectNode();
    reasoning.put("effort", reasoningEffort);
    root.set("reasoning", reasoning);

    ObjectNode audio = objectMapper.createObjectNode();
    ObjectNode output = objectMapper.createObjectNode();
    output.put("voice", voice);
    audio.set("output", output);

    ObjectNode input = objectMapper.createObjectNode();
    ObjectNode transcription = objectMapper.createObjectNode();
    transcription.put("model", interviewProperties.resolvedTranscriptionModel());
    input.set("transcription", transcription);

    ObjectNode turnDetection = objectMapper.createObjectNode();
    turnDetection.put("type", "semantic_vad");
    turnDetection.put("eagerness", vadEagerness);
    turnDetection.put("create_response", true);
    turnDetection.put("interrupt_response", true);
    input.set("turn_detection", turnDetection);

    audio.set("input", input);
    root.set("audio", audio);

    return objectMapper.writeValueAsString(root);
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

  private static String normalizeSdpLineEndings(String sdp) {
    return sdp.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\r\n");
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
