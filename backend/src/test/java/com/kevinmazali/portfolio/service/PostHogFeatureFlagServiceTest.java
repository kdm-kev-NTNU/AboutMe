package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.PostHogProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostHogFeatureFlagServiceTest {

  @Test
  void disabledWhenKeysEmpty() {
    PostHogProperties p = new PostHogProperties();
    p.setEnabled(true);
    p.setApiKey("phc_test");
    PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
    assertFalse(svc.isEnabled());
  }

  @Test
  void parsesDecideResponse() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/decide",
        exchange -> {
          byte[] body =
              "{\"featureFlags\":{\"aboutme_rag_feature_flags\":true,\"other\":false}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
          }
        });
    server.start();
    try {
      int port = server.getAddress().getPort();
      PostHogProperties p = new PostHogProperties();
      p.setEnabled(true);
      p.setApiKey("phc_test");
      p.setHost("http://127.0.0.1:" + port);
      p.setFeatureFlagKeys(List.of("aboutme_rag_feature_flags"));
      PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
      assertTrue(svc.isEnabled());
      Map<String, Object> m = svc.resolveForDistinctId("user:1");
      assertEquals(1, m.size());
      assertEquals(true, m.get("$feature/aboutme_rag_feature_flags"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void normalizeBaseUrlTrimsSlash() {
    assertEquals("https://eu.i.posthog.com", PostHogFeatureFlagService.normalizeBaseUrl("https://eu.i.posthog.com/"));
  }
}
