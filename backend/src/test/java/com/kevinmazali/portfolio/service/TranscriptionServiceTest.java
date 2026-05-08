package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TranscriptionServiceTest {

  @Mock
  private OpenAiAudioTranscriptionModel openAiModel;

  @Mock
  private ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModel;

  @Mock
  private AiBudgetService aiBudgetService;

  @Mock
  private AiCircuitBreaker aiCircuitBreaker;

  private AiBudgetProperties budgetProperties;
  private TranscriptionService service;

  @BeforeEach
  void setUp() {
    budgetProperties = new AiBudgetProperties();
    budgetProperties.setEnabled(false);
    when(transcriptionModel.getIfAvailable()).thenReturn(openAiModel);
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("  hello  ")));
    doNothing().when(aiCircuitBreaker).assertClosed();
    service = new TranscriptionService(transcriptionModel, aiBudgetService, budgetProperties, aiCircuitBreaker, "whisper-1");
  }

  @Test
  void transcribeReturnsTrimmedTextAndRecordsUsage() {
    ByteArrayResource res = new ByteArrayResource(new byte[32_000]);
    assertThat(service.transcribe(res, 32_000, null)).isEqualTo("hello");
    verify(aiBudgetService).recordUsage(
        anyString(),
        eq("whisper-1"),
        eq(2000),
        eq(0),
        anyBoolean(),
        anyDouble(),
        eq("audio_transcription"));
  }

  @Test
  void transcribePassesLanguageInPromptOptions() {
    ByteArrayResource res = new ByteArrayResource(new byte[16_000]);
    service.transcribe(res, 16_000, "en");
    ArgumentCaptor<AudioTranscriptionPrompt> cap = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
    verify(openAiModel).call(cap.capture());
    assertThat(cap.getValue().getOptions()).isInstanceOf(OpenAiAudioTranscriptionOptions.class);
    OpenAiAudioTranscriptionOptions opts = (OpenAiAudioTranscriptionOptions) cap.getValue().getOptions();
    assertThat(opts.getLanguage()).isEqualTo("en");
  }

  @Test
  void transcribeDefaultsToNorwegianWhenLanguageOmitted() {
    ByteArrayResource res = new ByteArrayResource(new byte[16_000]);
    service.transcribe(res, 16_000, null);
    ArgumentCaptor<AudioTranscriptionPrompt> cap = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
    verify(openAiModel).call(cap.capture());
    OpenAiAudioTranscriptionOptions opts = (OpenAiAudioTranscriptionOptions) cap.getValue().getOptions();
    assertThat(opts.getLanguage()).isEqualTo("no");
  }

  @Test
  void throwsWhenTranscriptionBeanMissing() {
    when(transcriptionModel.getIfAvailable()).thenReturn(null);
    TranscriptionService s = new TranscriptionService(transcriptionModel, aiBudgetService, budgetProperties, aiCircuitBreaker, "whisper-1");
    ByteArrayResource res = new ByteArrayResource(new byte[] { 1 });
    assertThatThrownBy(() -> s.transcribe(res, 1, null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void pseudoPromptTokensFromByteSize() {
    assertThat(TranscriptionService.pseudoPromptTokensFromByteSize(32_000)).isEqualTo(2000);
    assertThat(TranscriptionService.pseudoPromptTokensFromByteSize(1)).isEqualTo(1000);
  }

  // --- New tests below ---

  @Test
  void transcribeRejectsZeroByteFile() {
    ByteArrayResource res = new ByteArrayResource(new byte[] { 1 });
    assertThatThrownBy(() -> service.transcribe(res, 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void transcribeAcceptsExactly25MibBoundary() {
    ByteArrayResource res = new ByteArrayResource(new byte[] { 1, 2, 3 });
    long maxBytes = TranscriptionService.MAX_AUDIO_BYTES;
    assertThat(service.transcribe(res, maxBytes, null)).isEqualTo("hello");
  }

  @Test
  void transcribeRejectsAboveBoundary() {
    ByteArrayResource res = new ByteArrayResource(new byte[] { 1, 2, 3 });
    assertThatThrownBy(() -> service.transcribe(res, TranscriptionService.MAX_AUDIO_BYTES + 1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("25 MB");
  }

  @Test
  void transcribePropagatesBudgetExceeded() {
    doThrow(new BudgetExceededException("daily cap"))
        .when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    assertThatThrownBy(() -> service.transcribe(res, 1024, null))
        .isInstanceOf(BudgetExceededException.class);
    // Whisper must NEVER be called when budget is exceeded.
    verify(openAiModel, never()).call(any(AudioTranscriptionPrompt.class));
  }

  @Test
  void transcribePropagatesCircuitOpen() {
    doThrow(new AiCircuitOpenException("circuit"))
        .when(aiCircuitBreaker).assertClosed();
    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    assertThatThrownBy(() -> service.transcribe(res, 1024, null))
        .isInstanceOf(AiCircuitOpenException.class);
    verify(openAiModel, never()).call(any(AudioTranscriptionPrompt.class));
  }

  @Test
  void transcribeRetriesWithEnglishWhenNorwegianFails() {
    // First call (Norwegian primary) throws; second call (English fallback) succeeds.
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenThrow(new RuntimeException("openai 503"))
        .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("hello world")));

    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    String text = service.transcribe(res, 1024, "no");

    assertThat(text).isEqualTo("hello world");
    ArgumentCaptor<AudioTranscriptionPrompt> cap = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
    verify(openAiModel, times(2)).call(cap.capture());
    List<AudioTranscriptionPrompt> calls = cap.getAllValues();
    assertThat(((OpenAiAudioTranscriptionOptions) calls.get(0).getOptions()).getLanguage()).isEqualTo("no");
    assertThat(((OpenAiAudioTranscriptionOptions) calls.get(1).getOptions()).getLanguage()).isEqualTo("en");
  }

  @Test
  void transcribeRetriesWithNorwegianWhenEnglishFails() {
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenThrow(new RuntimeException("openai 502"))
        .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("hei")));

    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    String text = service.transcribe(res, 1024, "en");

    assertThat(text).isEqualTo("hei");
    ArgumentCaptor<AudioTranscriptionPrompt> cap = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
    verify(openAiModel, times(2)).call(cap.capture());
    assertThat(((OpenAiAudioTranscriptionOptions) cap.getAllValues().get(0).getOptions()).getLanguage()).isEqualTo("en");
    assertThat(((OpenAiAudioTranscriptionOptions) cap.getAllValues().get(1).getOptions()).getLanguage()).isEqualTo("no");
  }

  @Test
  void transcribePropagatesWhenBothLanguagesFail() {
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenThrow(new RuntimeException("primary fail"))
        .thenThrow(new RuntimeException("fallback fail"));

    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    assertThatThrownBy(() -> service.transcribe(res, 1024, "no"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("fallback fail");
    // Confirms it actually tried twice (the fallback) before giving up.
    verify(openAiModel, times(2)).call(any(AudioTranscriptionPrompt.class));
  }

  @Test
  void transcribeDoesNotRecordUsageWhenCallFailsCompletely() {
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenThrow(new RuntimeException("primary fail"))
        .thenThrow(new RuntimeException("fallback fail"));

    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    try {
      service.transcribe(res, 1024, "no");
    } catch (RuntimeException ignored) {
      // expected
    }
    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), anyDouble(), anyString());
  }

  @Test
  void fallbackLanguageHelperReturnsCorrectPair() {
    assertThat(TranscriptionService.fallbackLanguage("no")).isEqualTo("en");
    assertThat(TranscriptionService.fallbackLanguage("en")).isEqualTo("no");
    assertThat(TranscriptionService.fallbackLanguage("fr")).isNull();
    assertThat(TranscriptionService.fallbackLanguage(null)).isNull();
  }

  @Test
  void transcribeReturnsEmptyStringWhenModelOutputIsNull() {
    when(openAiModel.call(any(AudioTranscriptionPrompt.class)))
        .thenReturn(new AudioTranscriptionResponse(new AudioTranscription(null)));

    ByteArrayResource res = new ByteArrayResource(new byte[1024]);
    assertThat(service.transcribe(res, 1024, "no")).isEmpty();
  }
}
