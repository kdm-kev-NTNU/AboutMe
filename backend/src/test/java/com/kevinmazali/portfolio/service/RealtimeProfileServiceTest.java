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
        .contains("dataingenior");
    assertThat(service.profileCard("en"))
        .contains("Kevin")
        .contains("NTNU")
        .contains("data engineering");
  }

  @Test
  void lookupFindsEducationAndProjectFacts() {
    assertThat(service.lookup("NTNU data engineering", "en"))
        .extracting(snippet -> snippet.title())
        .anySatisfy(title -> assertThat(String.valueOf(title)).contains("NTNU"));

    assertThat(service.lookup("portefolje RAG", "no"))
        .extracting(snippet -> snippet.title())
        .anySatisfy(title -> assertThat(String.valueOf(title)).contains("Portefolje"));
  }
}
