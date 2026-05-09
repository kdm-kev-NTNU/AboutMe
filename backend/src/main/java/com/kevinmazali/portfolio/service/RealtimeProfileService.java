package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.model.RealtimeLookupSnippet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Loads the curated public profile card and structured voice facts from classpath JSON.
 */
@Service
public class RealtimeProfileService {

  private static final String PROFILE_PATH = "realtime/kevin-profile.json";
  private static final int DEFAULT_MAX_RESULTS = 5;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonNode root;

  public RealtimeProfileService() {
    this.root = loadProfile();
  }

  public String profileCard(String language) {
    String lang = normalizeLang(language);
    String card = root.path("languages").path(lang).path("profileCard").asText("");
    if (!StringUtils.hasText(card) && !"en".equals(lang)) {
      card = root.path("languages").path("en").path("profileCard").asText("");
    }
    return StringUtils.hasText(card) ? card.trim() : "";
  }

  public List<RealtimeLookupSnippet> lookup(String query, String language) {
    return lookup(query, language, DEFAULT_MAX_RESULTS);
  }

  public List<RealtimeLookupSnippet> lookup(String query, String language, int maxResults) {
    if (!StringUtils.hasText(query) || maxResults <= 0) {
      return List.of();
    }
    String lang = normalizeLang(language);
    String normalizedQuery = normalize(query);
    Set<String> terms = queryTerms(query);
    List<ScoredSnippet> scored = new ArrayList<>();
    JsonNode facts = root.path("facts");
    if (!facts.isArray()) {
      return List.of();
    }
    int order = 0;
    for (JsonNode fact : facts) {
      String title = localized(fact.path("title"), lang);
      String text = localized(fact.path("text"), lang);
      List<String> tags = tags(fact.path("tags"));
      int score = score(normalizedQuery, terms, title, text, tags);
      if (score > 0) {
        scored.add(new ScoredSnippet(
            new RealtimeLookupSnippet("profile", title, text), score, order));
      }
      order++;
    }
    return scored.stream()
        .sorted(Comparator.comparingInt(ScoredSnippet::score).reversed()
            .thenComparingInt(ScoredSnippet::order))
        .map(ScoredSnippet::snippet)
        .limit(Math.max(1, maxResults))
        .toList();
  }

  public static String normalizeLang(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "en";
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("no".equals(v) || "nb".equals(v) || "nn".equals(v)) {
      return "no";
    }
    return "en";
  }

  private JsonNode loadProfile() {
    try {
      var resource = new ClassPathResource(PROFILE_PATH);
      String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
      return objectMapper.readTree(json);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load realtime profile from " + PROFILE_PATH, e);
    }
  }

  private static String localized(JsonNode node, String lang) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    if (node.isTextual()) {
      return node.asText("").trim();
    }
    String value = node.path(lang).asText("");
    if (!StringUtils.hasText(value) && !"en".equals(lang)) {
      value = node.path("en").asText("");
    }
    if (!StringUtils.hasText(value) && !"no".equals(lang)) {
      value = node.path("no").asText("");
    }
    return value == null ? "" : value.trim();
  }

  private static List<String> tags(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (JsonNode item : node) {
      String tag = item.asText("");
      if (StringUtils.hasText(tag)) {
        result.add(tag.trim());
      }
    }
    return List.copyOf(result);
  }

  private static int score(
      String normalizedQuery, Set<String> terms, String title, String text, List<String> tags) {
    String titleNorm = normalize(title);
    String textNorm = normalize(text);
    List<String> tagNorms = tags.stream().map(RealtimeProfileService::normalize).toList();
    int score = 0;
    if (StringUtils.hasText(normalizedQuery)) {
      if (titleNorm.contains(normalizedQuery)) {
        score += 12;
      }
      if (tagNorms.stream().anyMatch(t -> t.contains(normalizedQuery))) {
        score += 10;
      }
      if (textNorm.contains(normalizedQuery)) {
        score += 6;
      }
    }
    for (String term : terms) {
      if (titleNorm.contains(term)) {
        score += 4;
      }
      if (tagNorms.stream().anyMatch(t -> t.contains(term))) {
        score += 3;
      }
      if (textNorm.contains(term)) {
        score += 1;
      }
    }
    return score;
  }

  private static Set<String> queryTerms(String raw) {
    Set<String> terms = new LinkedHashSet<>();
    for (String part : normalize(raw).split("[^\\p{L}\\p{N}]+")) {
      if (part.length() > 1 && !isStopWord(part)) {
        terms.add(part);
      }
    }
    return terms;
  }

  private static boolean isStopWord(String term) {
    return switch (term) {
      case "the", "and", "for", "with", "what", "does", "about", "tell", "his", "her", "him",
          "hva", "om", "og", "med", "kan", "du", "han", "hans", "kevin" -> true;
      default -> false;
    };
  }

  private static String normalize(String s) {
    return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
  }

  private record ScoredSnippet(RealtimeLookupSnippet snippet, int score, int order) {}
}
