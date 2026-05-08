package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
  void transcribeUsesNullOptionsWhenLanguageOmitted() {
    ByteArrayResource res = new ByteArrayResource(new byte[16_000]);
    service.transcribe(res, 16_000, null);
    ArgumentCaptor<AudioTranscriptionPrompt> cap = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
    verify(openAiModel).call(cap.capture());
    assertThat(cap.getValue().getOptions()).isNull();
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
}
