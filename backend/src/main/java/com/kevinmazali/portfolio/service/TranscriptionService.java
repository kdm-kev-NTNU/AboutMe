package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.util.AiRequestContext;
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
@Service
public class TranscriptionService {

  /** OpenAI Whisper API file size limit (25 MiB). */
  public static final long MAX_AUDIO_BYTES = 25L * 1024 * 1024;

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

    long startNs = System.nanoTime();
    OpenAiAudioTranscriptionOptions runtimeOptions = null;
    if (StringUtils.hasText(languageIso6391)) {
      String lang = languageIso6391.trim().toLowerCase(Locale.ROOT);
      runtimeOptions = OpenAiAudioTranscriptionOptions.builder()
          .language(lang)
          .build();
    }
    AudioTranscriptionPrompt prompt = runtimeOptions == null
        ? new AudioTranscriptionPrompt(audioResource)
        : new AudioTranscriptionPrompt(audioResource, runtimeOptions);

    AudioTranscriptionResponse response = model.call(prompt);
    String text = response.getResult() != null ? response.getResult().getOutput() : "";
    if (text == null) {
      text = "";
    }
    text = text.trim();

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
   * Maps compressed audio size to pseudo prompt tokens so {@link AiBudgetService#estimateCostUsd} stays in the right
   * ballpark for per-minute Whisper-style pricing (see {@code portfolio.ai.budget.models.whisper-1}).
   */
  static int pseudoPromptTokensFromByteSize(long bytes) {
    long estSeconds = Math.max(1L, bytes / 16_000L);
    long tokens = estSeconds * 1000L;
    return (int) Math.min(Integer.MAX_VALUE, tokens);
  }
}
