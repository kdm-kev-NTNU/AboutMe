package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

  @Test
  void resolve_usesLeftmostForwardedForHeader() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("10.0.0.1");
    req.addHeader("X-Forwarded-For", "198.51.100.7, 203.0.113.5");
    assertEquals("198.51.100.7", ClientIpResolver.resolve(req));
  }

  @Test
  void resolve_fallsBackToRemoteAddr() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("203.0.113.5");
    assertEquals("203.0.113.5", ClientIpResolver.resolve(req));
  }
}
