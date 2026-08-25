package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RealtimeProfileServiceTest {

  private final RealtimeProfileService service = new RealtimeProfileService();

  @Test
  void profileCardLoadsBothLanguages() {
    assertThat(service.profileCard("no"))
        .contains("Kevin")
        .contains("NTNU")
        .contains("dataingeniør");
    assertThat(service.profileCard("en"))
        .contains("Kevin")
        .contains("NTNU")
        .contains("data engineering");
  }

  @Test
  void fieldMatchesToleratesMinorTranscriptionTypos() {
    assertThat(RealtimeProfileService.fieldMatches("kevin studies data engineering at ntnu", "enginering"))
        .isTrue();
    assertThat(RealtimeProfileService.levenshteinDistance("prosjekter", "projeckter")).isLessThanOrEqualTo(2);
  }

  @Test
  void lookupFindsEducationAndProjectFacts() {
    assertThat(service.lookup("NTNU data engineering", "en"))
        .extracting(snippet -> snippet.title())
        .anySatisfy(title -> assertThat(String.valueOf(title)).contains("NTNU"));

    assertThat(service.lookup("portefolje RAG", "no"))
        .extracting(snippet -> snippet.title())
        .anySatisfy(title -> assertThat(String.valueOf(title)).contains("Portefølje"));
  }

  @Test
  void lookupFindsCompletedSpareBankInternship() {
    assertThat(service.lookup("SpareBank 1 Utvikling sommerjobb SIFO", "no"))
        .extracting(snippet -> snippet.text())
        .anySatisfy(text -> assertThat(String.valueOf(text))
            .contains("regelbasert anbefalingssystem")
            .contains("SIFOs referansebudsjett")
            .doesNotContain("skal gjennomføre"));

    assertThat(service.lookup("SpareBank 1 housing savings recommendation", "en"))
        .extracting(snippet -> snippet.text())
        .anySatisfy(text -> assertThat(String.valueOf(text))
            .contains("rule-based recommendation system")
            .contains("SIFO")
            .doesNotContain("scheduled to complete"));
  }
}
