package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.PhoenixProperties;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetExample;
import com.kevinmazali.portfolio.model.experiment.PhoenixDatasetSummary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phoenix dataset REST API ({@code /v1/datasets*}) for eval experiments.
 */
@Service
public class PhoenixDatasetService {

  private static final int LIST_PAGE_SIZE = 100;

  private final PhoenixProperties phoenixProperties;
  private final ObjectMapper objectMapper;

  public PhoenixDatasetService(PhoenixProperties phoenixProperties, ObjectMapper objectMapper) {
    this.phoenixProperties = phoenixProperties;
    this.objectMapper = objectMapper;
  }

  public boolean isEnabled() {
    return phoenixProperties.isConfigured();
  }

  public List<PhoenixDatasetSummary> listDatasets() {
    requireConfigured();
    List<PhoenixDatasetSummary> out = new ArrayList<>();
    String cursor = null;
    do {
      UriComponentsBuilder ub = UriComponentsBuilder.fromPath("/v1/datasets")
          .queryParam("limit", LIST_PAGE_SIZE);
      if (StringUtils.hasText(cursor)) {
        ub.queryParam("cursor", cursor);
      }
      URI uri = ub.build().toUri();
      JsonNode root = phoenixGet(uri);
      JsonNode data = root.path("data");
      if (data.isArray()) {
        for (JsonNode row : data) {
          String id = textOrEmpty(row, "id");
          String name = textOrEmpty(row, "name");
          int count = row.path("example_count").asInt(0);
          if (StringUtils.hasText(id)) {
            out.add(new PhoenixDatasetSummary(id, name, count));
          }
        }
      }
      JsonNode next = root.get("next_cursor");
      cursor = (next != null && !next.isNull() && next.isTextual()) ? next.asText() : null;
    } while (cursor != null && !cursor.isBlank());
    return out;
  }

  public Optional<PhoenixDatasetSummary> findByName(String datasetName) {
    if (!StringUtils.hasText(datasetName)) {
      return Optional.empty();
    }
    return listDatasets().stream()
        .filter(d -> datasetName.trim().equals(d.name()))
        .findFirst();
  }

  public List<PhoenixDatasetExample> getExamples(String datasetGlobalId) {
    requireConfigured();
    JsonNode root = phoenixGet(UriComponentsBuilder.fromPath("/v1/datasets/{id}/examples")
        .buildAndExpand(datasetGlobalId)
        .toUri());
    JsonNode examples = root.path("data").path("examples");
    if (!examples.isArray()) {
      return List.of();
    }
    List<PhoenixDatasetExample> out = new ArrayList<>();
    for (JsonNode ex : examples) {
      JsonNode input = ex.path("input");
      JsonNode output = ex.path("output");
      String question = textFromNested(input, "question");
      String ref = textFromNested(output, "reference_text");
      Map<String, Object> inMap = jsonNodeToMap(input);
      Map<String, Object> outMap = jsonNodeToMap(output);
      out.add(new PhoenixDatasetExample(question, ref, inMap, outMap));
    }
    return out;
  }

  public void deleteDataset(String datasetGlobalId) {
    requireConfigured();
    try {
      restClient().delete()
          .uri("/v1/datasets/{id}", datasetGlobalId)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      throw new IllegalStateException("Phoenix delete failed: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a dataset via {@code POST /v1/datasets/upload?sync=true} (JSON body).
   */
  public PhoenixDatasetSummary createDataset(
      String name,
      String description,
      List<Map<String, Object>> inputs,
      List<Map<String, Object>> outputs) {
    requireConfigured();
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Dataset name is required");
    }
    if (inputs == null || inputs.isEmpty()) {
      throw new IllegalArgumentException("inputs must be non-empty");
    }
    List<Map<String, Object>> outs = outputs;
    if (outs == null || outs.size() != inputs.size()) {
      outs = new ArrayList<>();
      for (int i = 0; i < inputs.size(); i++) {
        outs.add(new HashMap<>());
      }
    }
    Map<String, Object> body = new HashMap<>();
    body.put("name", name.trim());
    body.put("description", description != null ? description : "");
    body.put("action", "create");
    body.put("inputs", inputs);
    body.put("outputs", outs);

    JsonNode root;
    try {
      root = restClient().post()
          .uri("/v1/datasets/upload?sync=true")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException e) {
      throw new IllegalStateException("Phoenix dataset create failed: " + e.getMessage(), e);
    }
    if (root == null) {
      throw new IllegalStateException("Phoenix returned empty body");
    }
    JsonNode data = root.path("data");
    String id = textOrEmpty(data, "dataset_id");
    if (!StringUtils.hasText(id)) {
      throw new IllegalStateException("Phoenix response missing dataset_id: " + root);
    }
    return new PhoenixDatasetSummary(id, name.trim(), inputs.size());
  }

  private void requireConfigured() {
    if (!phoenixProperties.isConfigured()) {
      throw new IllegalStateException(
          "Phoenix REST is not configured. Set portfolio.phoenix.base-url (e.g. PHOENIX_BASE_URL).");
    }
  }

  private RestClient restClient() {
    String base = phoenixProperties.getBaseUrl().trim().replaceAll("/+$", "");
    RestClient.Builder b = RestClient.builder().baseUrl(base);
    if (StringUtils.hasText(phoenixProperties.getApiKey())) {
      String token = phoenixProperties.getApiKey().trim();
      b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      b.defaultHeader("x-api-key", token);
    }
    return b.build();
  }

  private JsonNode phoenixGet(URI uri) {
    try {
      String path = uri.getRawPath();
      String query = uri.getRawQuery();
      String pathAndQuery = query != null ? path + "?" + query : path;
      JsonNode body = restClient().get()
          .uri(pathAndQuery)
          .retrieve()
          .body(JsonNode.class);
      return body != null ? body : objectMapper.createObjectNode();
    } catch (RestClientException e) {
      throw new IllegalStateException("Phoenix GET failed (" + uri + "): " + e.getMessage(), e);
    }
  }

  private static String textOrEmpty(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() ? "" : v.asText("");
  }

  private static String textFromNested(JsonNode obj, String key) {
    if (obj == null || !obj.isObject()) {
      return "";
    }
    JsonNode v = obj.get(key);
    return v == null || v.isNull() ? "" : v.asText("").trim();
  }

  private Map<String, Object> jsonNodeToMap(JsonNode node) {
    if (node == null || node.isNull() || !node.isObject()) {
      return Collections.emptyMap();
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = objectMapper.convertValue(node, Map.class);
      return m != null ? m : Collections.emptyMap();
    } catch (IllegalArgumentException e) {
      return Collections.emptyMap();
    }
  }
}
