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
    assertThat(p.getVoice()).isEqualTo("marin");
    assertThat(p.getReasoningEffort()).isEqualTo("low");
    assertThat(p.getMaxResponseOutputTokens()).isEqualTo(1024);
    assertThat(p.getReservationInputTokens()).isEqualTo(2000);
    assertThat(p.getReservationOutputTokens()).isEqualTo(2000);
    assertThat(p.defaultVoice()).isEqualTo("marin");
    assertThat(p.defaultReasoningEffort()).isEqualTo("low");
    assertThat(RealtimeProperties.ALLOWED_VOICES).containsExactly("marin", "cedar");
    assertThat(RealtimeProperties.ALLOWED_REASONING_EFFORTS).containsExactly("low", "medium", "high");
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

  @Test
  void curated_voice_and_reasoning_values_are_normalized_and_validated() {
    RealtimeProperties p = new RealtimeProperties();
    p.setVoice("cedar");
    p.setReasoningEffort("medium");

    assertThat(p.resolveVoice(null)).isEqualTo("cedar");
    assertThat(p.resolveVoice(" MARIN ")).isEqualTo("marin");
    assertThat(p.resolveVoice("bad")).isEqualTo("cedar");
    assertThat(p.isAllowedVoice(null)).isTrue();
    assertThat(p.isAllowedVoice(" cedar ")).isTrue();
    assertThat(p.isAllowedVoice("alloy")).isFalse();

    assertThat(p.resolveReasoningEffort(null)).isEqualTo("medium");
    assertThat(p.resolveReasoningEffort(" HIGH ")).isEqualTo("high");
    assertThat(p.resolveReasoningEffort("xhigh")).isEqualTo("medium");
    assertThat(p.isAllowedReasoningEffort(null)).isTrue();
    assertThat(p.isAllowedReasoningEffort("low")).isTrue();
    assertThat(p.isAllowedReasoningEffort("minimal")).isFalse();
  }

  @Test
  void invalid_config_defaults_fall_back_to_product_defaults() {
    RealtimeProperties p = new RealtimeProperties();
    p.setVoice("alloy");
    p.setReasoningEffort("xhigh");

    assertThat(p.defaultVoice()).isEqualTo("marin");
    assertThat(p.defaultReasoningEffort()).isEqualTo("low");
  }
}
