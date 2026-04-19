package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoenixDatasetServiceTest {

  private HttpServer server;
  private String baseUrl;
  private PhoenixProperties props;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void startServer() throws IOException {
    props = new PhoenixProperties();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.setExecutor(null);
    server.start();
    int port = server.getAddress().getPort();
    baseUrl = "http://127.0.0.1:" + port;
    props.setBaseUrl(baseUrl);
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  private PhoenixDatasetService service() {
    return new PhoenixDatasetService(props, objectMapper);
  }

  private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "application/json");
    ex.sendResponseHeaders(status, body.length);
    ex.getResponseBody().write(body);
    ex.close();
  }

  private static String readBody(HttpExchange ex) throws IOException {
    try (InputStream in = ex.getRequestBody()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void isEnabledFalseWhenBaseUrlBlank() {
    props.setBaseUrl("  ");
    assertFalse(service().isEnabled());
  }

  @Test
  void listDatasetsThrowsWhenNotConfigured() {
    props.setBaseUrl("");
    assertThrows(IllegalStateException.class, () -> service().listDatasets());
  }

  @Test
  void findByNameReturnsEmptyForBlankName() {
    assertEquals(Optional.empty(), service().findByName(null));
    assertEquals(Optional.empty(), service().findByName(""));
    assertEquals(Optional.empty(), service().findByName("   "));
  }

  @Test
  void listDatasetsParsesRowsAndStopsOnPagination() throws IOException {
    AtomicInteger page = new AtomicInteger();
    server.createContext("/", ex -> {
      try {
        if (!"GET".equals(ex.getRequestMethod())) {
          sendJson(ex, 405, "{}");
          return;
        }
        String q = ex.getRequestURI().getQuery();
        int p = page.getAndIncrement();
        if (p == 0) {
          assertTrue(q != null && q.contains("limit=100"));
          assertTrue(q == null || !q.contains("cursor="));
          String json = """
              {"data":[{"id":"a","name":"Alpha","example_count":2}],"next_cursor":"c1"}
              """;
          sendJson(ex, 200, json);
        } else {
          assertTrue(q != null && q.contains("cursor=c1"));
          sendJson(ex, 200, """
              {"data":[{"id":"b","name":"Beta","example_count":1}],"next_cursor":null}
              """);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    List<PhoenixDatasetSummary> list = service().listDatasets();
    assertEquals(2, list.size());
    assertEquals("a", list.get(0).id());
    assertEquals("Alpha", list.get(0).name());
    assertEquals(2, list.get(0).exampleCount());
    assertEquals("b", list.get(1).id());
  }

  @Test
  void findByNameMatchesPhoenixNameUsingTrimmedSearchString() throws IOException {
    server.createContext("/", ex -> {
      try {
        sendJson(ex, 200, """
            {"data":[{"id":"x","name":"Alpha","example_count":0}]}
            """);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    Optional<PhoenixDatasetSummary> found = service().findByName("  Alpha  ");
    assertTrue(found.isPresent());
    assertEquals("x", found.get().id());
  }

  @Test
  void getExamplesParsesNestedInputOutput() throws IOException {
    server.createContext("/", ex -> {
      try {
        String json = """
            {"data":{"examples":[
              {"input":{"question":" Q1 "},"output":{"reference_text":" A1 "}}
            ]}}
            """;
        sendJson(ex, 200, json);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    List<PhoenixDatasetExample> exs = service().getExamples("ds-global");
    assertEquals(1, exs.size());
    assertEquals("Q1", exs.get(0).question());
    assertEquals("A1", exs.get(0).referenceText());
    assertFalse(exs.get(0).rawInput().isEmpty());
  }

  @Test
  void getExamplesReturnsEmptyWhenExamplesNotArray() throws IOException {
    server.createContext("/", ex -> {
      try {
        sendJson(ex, 200, "{\"data\":{\"examples\":\"nope\"}}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertTrue(service().getExamples("id").isEmpty());
  }

  @Test
  void createDatasetRejectsBlankName() {
    assertThrows(IllegalArgumentException.class,
        () -> service().createDataset("  ", null, List.of(Map.of("question", "q")), null));
  }

  @Test
  void createDatasetRejectsEmptyInputs() {
    assertThrows(IllegalArgumentException.class,
        () -> service().createDataset("n", null, List.of(), null));
  }

  @Test
  void createDatasetPadsOutputsWhenMissing() throws IOException {
    AtomicReference<String> posted = new AtomicReference<>();
    server.createContext("/", ex -> {
      try {
        if ("POST".equals(ex.getRequestMethod())) {
          posted.set(readBody(ex));
          sendJson(ex, 200, "{\"data\":{\"dataset_id\":\"new-id\"}}");
        } else {
          sendJson(ex, 404, "{}");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    PhoenixDatasetSummary s = service().createDataset(
        "MySet",
        "desc",
        List.of(Map.of("q", 1), Map.of("q", 2)),
        null);
    assertEquals("new-id", s.id());
    assertEquals("MySet", s.name());
    assertEquals(2, s.exampleCount());

    @SuppressWarnings("unchecked")
    Map<String, Object> body = objectMapper.readValue(posted.get(), Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> outs = (List<Map<String, Object>>) body.get("outputs");
    assertEquals(2, outs.size());
    assertTrue(outs.get(0).isEmpty());
    assertTrue(outs.get(1).isEmpty());
  }

  @Test
  void createDatasetThrowsWhenResponseMissingDatasetId() throws IOException {
    server.createContext("/", ex -> {
      try {
        sendJson(ex, 200, "{\"data\":{}}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertThrows(IllegalStateException.class,
        () -> service().createDataset("n", null, List.of(Map.of("a", "b")), List.of(Map.of())));
  }

  @Test
  void deleteDatasetPropagatesHttpFailure() throws IOException {
    server.createContext("/", ex -> {
      try {
        ex.sendResponseHeaders(500, 0);
        ex.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertThrows(IllegalStateException.class, () -> service().deleteDataset("any"));
  }

  @Test
  void apiKeyAddsAuthHeaders() throws IOException {
    props.setApiKey("secret-key");
    server.createContext("/", ex -> {
      assertEquals("Bearer secret-key", ex.getRequestHeaders().getFirst("Authorization"));
      assertEquals("secret-key", ex.getRequestHeaders().getFirst("x-api-key"));
      sendJson(ex, 200, "{\"data\":[]}");
    });

    service().listDatasets();
  }
}
