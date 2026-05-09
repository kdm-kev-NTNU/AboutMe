package com.kevinmazali.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RealtimeRateLimitPropertiesTest {

  @Test
  void defaults_match_application_yaml_contract() {
    RealtimeRateLimitProperties p = new RealtimeRateLimitProperties();

    assertThat(p.isEnabled()).isTrue();
    assertThat(p.getCapacity()).isEqualTo(3);
    assertThat(p.getWindowSeconds()).isEqualTo(3600);
  }

  @Test
  void getters_reflect_setters_roundTrip() {
    RealtimeRateLimitProperties p = new RealtimeRateLimitProperties();

    p.setEnabled(false);
    p.setCapacity(10);
    p.setWindowSeconds(120);

    assertThat(p.isEnabled()).isFalse();
    assertThat(p.getCapacity()).isEqualTo(10);
    assertThat(p.getWindowSeconds()).isEqualTo(120);
  }
}
