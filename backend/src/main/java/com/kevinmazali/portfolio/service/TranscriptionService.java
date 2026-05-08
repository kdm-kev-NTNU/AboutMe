package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.util.AiRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * OpenAI speech-to-text via Spring AI {@link OpenAiAudioTranscriptionModel}.
 */
@Slf4j
@Service
public class TranscriptionService {

  /** OpenAI Whisper API file size limit (25 MiB). */
  public static final long MAX_AUDIO_BYTES = 25L * 1024 * 1024;

  /** Default primary language hint when caller does not specify one. Matches YAML config. */
  static final String DEFAULT_LANGUAGE = "no";

  private final ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModel;
  private final AiBudgetService aiBudgetService;
  private final AiBudgetProperties budgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final String budgetModelId;

  public TranscriptionService(
      ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModel,
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      @Value("${spring.ai.openai.audio.transcription.options.model:whisper-1}") String budgetModelId) {
    this.transcriptionModel = transcriptionModel;
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.budgetModelId = budgetModelId;
  }

  /**
   * Transcribes audio to plain text. Enforces AI budget and circuit breaker; records estimated usage from byte size.
   *
   * <p>If the first call to OpenAI fails with a transient/unknown error, retries once with the
   * <em>other</em> supported language ({@code no}↔{@code en}). This salvages cases where the user's
   * spoken language doesn't match the UI language hint (e.g. UI is Norwegian but the user spoke
   * English, which can cause Whisper to return garbage or 4xx).
   *
   * <p>Budget/circuit failures and validation errors are <strong>not</strong> retried; they
   * propagate immediately.
   *
   * @param languageIso6391 optional ISO-639-1 override (e.g. {@code en}, {@code no}); merged into runtime options when set
   */
  public String transcribe(Resource audioResource, long fileSizeBytes, String languageIso6391) {
    OpenAiAudioTranscriptionModel model = transcriptionModel.getIfAvailable();
    if (model == null) {
      throw new IllegalStateException("OpenAI transcription is not configured (missing API key or disabled model).");
    }
    if (fileSizeBytes <= 0) {
      throw new IllegalArgumentException("Audio file is empty.");
    }
    if (fileSizeBytes > MAX_AUDIO_BYTES) {
      throw new IllegalArgumentException("Audio file exceeds maximum size of 25 MB.");
    }

    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    aiCircuitBreaker.assertClosed();
    aiBudgetService.assertWithinBudget(budgetUserId, anonymous);

    String primaryLang = StringUtils.hasText(languageIso6391)
        ? languageIso6391.trim().toLowerCase(Locale.ROOT)
        : DEFAULT_LANGUAGE;
    String fallbackLang = fallbackLanguage(primaryLang);

    long startNs = System.nanoTime();
    String text;
    try {
      text = invokeModel(model, audioResource, primaryLang);
    } catch (RuntimeException primaryFailure) {
      // Don't retry on cost/control failures — they will recur and just burn quota.
      if (primaryFailure instanceof BudgetExceededException
          || primaryFailure instanceof AiCircuitOpenException) {
        throw primaryFailure;
      }
      if (fallbackLang == null) {
        throw primaryFailure;
      }
      log.warn(
          "/transcribe primary language '{}' failed, retrying with fallback '{}': {}",
          primaryLang,
          fallbackLang,
          primaryFailure.getMessage());
      text = invokeModel(model, audioResource, fallbackLang);
    }

    double latencySec = (System.nanoTime() - startNs) / 1_000_000_000.0;
    int pseudoPromptTokens = pseudoPromptTokensFromByteSize(fileSizeBytes);
    aiBudgetService.recordUsage(
        budgetUserId,
        budgetModelId,
        pseudoPromptTokens,
        0,
        anonymous,
        latencySec,
        "audio_transcription");

    return text;
  }

  /**
   * Calls Spring AI with an explicit language hint and returns trimmed text.
   */
  private static String invokeModel(
      OpenAiAudioTranscriptionModel model, Resource audioResource, String languageIso6391) {
    OpenAiAudioTranscriptionOptions runtimeOptions = OpenAiAudioTranscriptionOptions.builder()
        .language(languageIso6391)
        .build();
    AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, runtimeOptions);
    AudioTranscriptionResponse response = model.call(prompt);
    String text = response.getResult() != null ? response.getResult().getOutput() : "";
    if (text == null) {
      text = "";
    }
    return text.trim();
  }

  /**
   * Returns the alternate supported language for retry, or {@code null} if {@code primary} is not
   * a supported pair. Currently only {@code no}↔{@code en} is supported, matching the public API.
   */
  static String fallbackLanguage(String primary) {
    if ("no".equals(primary)) {
      return "en";
    }
    if ("en".equals(primary)) {
      return "no";
    }
    return null;
  }

  /**
   * Maps compressed audio size to pseudo prompt tokens so {@link AiBudgetService#estimateCostUsd} stays in the right
   * ballpark for per-minute Whisper-style pricing (see {@code portfolio.ai.budget.models.whisper-1}).
   */
  static int pseudoPromptTokensFromByteSize(long bytes) {
    long estSeconds = Math.max(1L, bytes / 16_000L);
    long tokens = estSeconds * 1000L;
    return (int) Math.min(Integer.MAX_VALUE, tokens);
  }
}
