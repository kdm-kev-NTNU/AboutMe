package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.model.interview.InterviewTurnDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.openai.OpenAiChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InterviewTranscriptCleanerServiceTest {

  @Test
  void structureRawTranscriptFormatsQa() {
    var service =
        new InterviewTranscriptCleanerService(
            new NoiseCleaner(),
            mock(ObjectProvider.class),
            mock(ObjectProvider.class),
            mock(AiBudgetService.class),
            new AiBudgetProperties(),
            mock(AiCircuitBreaker.class));

    String raw =
        service.structureRawTranscript(
            List.of(
                new InterviewTurnDto("interviewer", "Tell me about your studies.", 0),
                new InterviewTurnDto("user", "I study computer science at NTNU.", 1)));

    assertThat(raw).contains("## Interviewer");
    assertThat(raw).contains("## Kevin");
    assertThat(raw).contains("computer science");
  }

  @Test
  void cleanForIngestRunsNoiseCleanerWithoutLlm() {
    var openAiProvider = mock(ObjectProvider.class);
    org.mockito.Mockito.when(openAiProvider.getIfAvailable()).thenReturn(null);

    var service =
        new InterviewTranscriptCleanerService(
            new NoiseCleaner(),
            mock(ObjectProvider.class),
            openAiProvider,
            mock(AiBudgetService.class),
            new AiBudgetProperties(),
            mock(AiCircuitBreaker.class));

    String cleaned = service.cleanForIngest("Kevin studies at NTNU.\n\nPage 1 of 1\n", "en");
    assertThat(cleaned).contains("NTNU");
    assertThat(cleaned).doesNotContain("Page 1 of 1");
  }
}
