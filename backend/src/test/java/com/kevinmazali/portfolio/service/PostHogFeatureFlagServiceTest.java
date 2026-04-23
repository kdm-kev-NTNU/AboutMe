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

  @Test
  void normalizeBaseUrl_nullFallsBackToDefaultHost() {
    assertEquals("https://eu.i.posthog.com", PostHogFeatureFlagService.normalizeBaseUrl(null));
  }

  @Test
  void resolveUsesUnknownWhenDistinctIdBlank() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/decide",
        exchange -> {
          String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          assertTrue(body.contains("\"distinct_id\":\"unknown\""));
          byte[] out = "{\"featureFlags\":{\"f\":42}}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
          }
        });
    server.start();
    try {
      int port = server.getAddress().getPort();
      PostHogProperties p = new PostHogProperties();
      p.setEnabled(true);
      p.setApiKey("phc_test");
      p.setHost("http://127.0.0.1:" + port);
      p.setFeatureFlagKeys(List.of("f"));
      PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
      Map<String, Object> m = svc.resolveForDistinctId("   ");
      assertEquals(42, m.get("$feature/f"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void resolveReturnsEmptyWhenFeatureFlagsNotObject() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/decide",
        exchange -> {
          byte[] out = "{\"featureFlags\":[]}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
          }
        });
    server.start();
    try {
      PostHogProperties p = new PostHogProperties();
      p.setEnabled(true);
      p.setApiKey("phc_test");
      p.setHost("http://127.0.0.1:" + server.getAddress().getPort());
      p.setFeatureFlagKeys(List.of("f"));
      PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
      assertTrue(svc.resolveForDistinctId("u").isEmpty());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void resolveMapsStringNumberAndJsonFallbackValues() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/decide",
        exchange -> {
          byte[] out =
              "{\"featureFlags\":{\"s\":\"beta\",\"n\":7,\"j\":{\"nested\":1}}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
          }
        });
    server.start();
    try {
      PostHogProperties p = new PostHogProperties();
      p.setEnabled(true);
      p.setApiKey("phc_test");
      p.setHost("http://127.0.0.1:" + server.getAddress().getPort());
      p.setFeatureFlagKeys(List.of("s", "n", "j", "", " "));
      PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
      Map<String, Object> m = svc.resolveForDistinctId("u");
      assertEquals("beta", m.get("$feature/s"));
      assertEquals(7, m.get("$feature/n"));
      assertTrue(m.get("$feature/j").toString().contains("nested"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void resolveReturnsEmptyOnTransportFailure() {
    PostHogProperties p = new PostHogProperties();
    p.setEnabled(true);
    p.setApiKey("phc_test");
    p.setHost("http://127.0.0.1:9");
    p.setFeatureFlagKeys(List.of("f"));
    PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
    assertTrue(svc.isEnabled());
    assertTrue(svc.resolveForDistinctId("u").isEmpty());
  }

  @Test
  void resolveReturnsEmptyWhenDecideBodyBlank() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/decide",
        exchange -> {
          byte[] out = "   \n".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
          }
        });
    server.start();
    try {
      PostHogProperties p = new PostHogProperties();
      p.setEnabled(true);
      p.setApiKey("phc_test");
      p.setHost("http://127.0.0.1:" + server.getAddress().getPort());
      p.setFeatureFlagKeys(List.of("f"));
      PostHogFeatureFlagService svc = new PostHogFeatureFlagService(p, new ObjectMapper());
      assertTrue(svc.resolveForDistinctId("u").isEmpty());
    } finally {
      server.stop(0);
    }
  }
}
