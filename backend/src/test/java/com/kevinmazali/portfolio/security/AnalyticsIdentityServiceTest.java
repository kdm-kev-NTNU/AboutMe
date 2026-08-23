package com.kevinmazali.portfolio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kevinmazali.portfolio.config.PostHogProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsIdentityServiceTest {

  private AnalyticsIdentityService service;

  @BeforeEach
  void setUp() {
    PostHogProperties properties = new PostHogProperties();
    properties.setIdentitySalt("test-salt");
    service = new AnalyticsIdentityService(properties);
  }

  @Test
  void distinctIdFor_isDeterministicForSameUsername() {
    String first = service.distinctIdFor("admin");
    String second = service.distinctIdFor("admin");
    assertEquals(first, second);
  }

  @Test
  void distinctIdFor_differsForDifferentUsernames() {
    assertNotEquals(service.distinctIdFor("alice"), service.distinctIdFor("bob"));
  }

  @Test
  void distinctIdFor_neverEqualsUsername() {
    String id = service.distinctIdFor("admin");
    assertNotEquals("admin", id);
    assertTrue(id.startsWith("owner_"));
  }

  @Test
  void distinctIdFor_stableAcrossServiceInstances() {
    PostHogProperties properties = new PostHogProperties();
    properties.setIdentitySalt("test-salt");
    AnalyticsIdentityService other = new AnalyticsIdentityService(properties);
    assertEquals(service.distinctIdFor("admin"), other.distinctIdFor("admin"));
  }
}
