package com.kevinmazali.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for realtime feature defaults bound from {@link RealtimeProperties}. */
class RealtimePropertiesTest {

  @Test
  void defaults_match_product_expectations() {
    RealtimeProperties p = new RealtimeProperties();

    assertThat(p.isEnabled()).isFalse();
    assertThat(p.getModel()).isEqualTo("gpt-realtime-2");
    assertThat(p.getVoice()).isEqualTo("ash");
    assertThat(p.getReasoningEffort()).isEqualTo("medium");
    assertThat(p.getMaxResponseOutputTokens()).isEqualTo(1024);
    assertThat(p.getReservationInputTokens()).isEqualTo(2000);
    assertThat(p.getReservationOutputTokens()).isEqualTo(2000);
  }

  @Test
  void getters_reflect_setters_roundTrip() {
    RealtimeProperties p = new RealtimeProperties();

    p.setEnabled(true);
    p.setModel("m-x");
    p.setVoice("fable");
    p.setReasoningEffort("low");
    p.setMaxResponseOutputTokens(99);
    p.setReservationInputTokens(111);
    p.setReservationOutputTokens(222);

    assertThat(p.isEnabled()).isTrue();
    assertThat(p.getModel()).isEqualTo("m-x");
    assertThat(p.getVoice()).isEqualTo("fable");
    assertThat(p.getReasoningEffort()).isEqualTo("low");
    assertThat(p.getMaxResponseOutputTokens()).isEqualTo(99);
    assertThat(p.getReservationInputTokens()).isEqualTo(111);
    assertThat(p.getReservationOutputTokens()).isEqualTo(222);
  }
}
