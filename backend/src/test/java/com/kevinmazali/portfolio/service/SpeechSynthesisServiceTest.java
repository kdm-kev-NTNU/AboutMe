package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeechSynthesisServiceTest {

  @Mock
  private AiBudgetService aiBudgetService;

  @Mock
  private AiCircuitBreaker aiCircuitBreaker;

  @Mock
  private OpenAiSpeechHttpInvoker openAiSpeechHttpInvoker;

  @Mock
  private HttpResponse<byte[]> httpResponse;

  private SpeechSynthesisService service;

  @BeforeEach
  void setUp() {
    AiBudgetProperties budgetProperties = new AiBudgetProperties();
    budgetProperties.setEnabled(false);
    service = new SpeechSynthesisService(
        aiBudgetService,
        budgetProperties,
        aiCircuitBreaker,
        openAiSpeechHttpInvoker,
        "sk-test",
        "tts-1",
        "nova");
  }

  @Test
  void synthesizeReturnsAudioAndRecordsUsage() throws Exception {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new byte[] {1, 2});
    when(openAiSpeechHttpInvoker.invoke(any(HttpRequest.class))).thenReturn(httpResponse);

    byte[] result = service.synthesize("Hei", "no");

    assertThat(result).containsExactly(1, 2);
    verify(aiBudgetService).recordUsage(
        anyString(),
        eq("tts-1"),
        eq(3),
        eq(0),
        anyBoolean(),
        anyDouble(),
        eq("audio_synthesis"));
  }

  @Test
  void synthesizeRejectsBlankText() {
    assertThatThrownBy(() -> service.synthesize("  ", "en"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Text is required");
  }

  @Test
  void synthesizePropagatesBudgetExceeded() {
    doThrow(new BudgetExceededException("cap"))
        .when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
    assertThatThrownBy(() -> service.synthesize("hello", "en"))
        .isInstanceOf(BudgetExceededException.class);
    verifyNoInteractions(openAiSpeechHttpInvoker);
  }

  @Test
  void synthesizePropagatesCircuitOpen() {
    doThrow(new AiCircuitOpenException("circuit"))
        .when(aiCircuitBreaker).assertClosed();
    assertThatThrownBy(() -> service.synthesize("hello", "en"))
        .isInstanceOf(AiCircuitOpenException.class);
    verifyNoInteractions(openAiSpeechHttpInvoker);
  }

  @Test
  void synthesizeThrowsWhenOpenAiRejects() throws Exception {
    when(httpResponse.statusCode()).thenReturn(400);
    when(httpResponse.body()).thenReturn("{\"error\":\"bad\"}".getBytes());
    when(openAiSpeechHttpInvoker.invoke(any(HttpRequest.class))).thenReturn(httpResponse);

    assertThatThrownBy(() -> service.synthesize("hello", "en"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OpenAI speech API failed");
  }
}
