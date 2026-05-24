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
  /** Minimum keyword score for a profile fact to be returned as relevant. */
  static final int MIN_RELEVANCE_SCORE = 4;

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

  public record ProfileLookupResult(List<RealtimeLookupSnippet> snippets, int bestScore) {}

  public List<RealtimeLookupSnippet> lookup(String query, String language) {
    return lookupDetailed(query, language, DEFAULT_MAX_RESULTS).snippets();
  }

  public List<RealtimeLookupSnippet> lookup(String query, String language, int maxResults) {
    return lookupDetailed(query, language, maxResults).snippets();
  }

  public ProfileLookupResult lookupDetailed(String query, String language, int maxResults) {
    if (!StringUtils.hasText(query) || maxResults <= 0) {
      return new ProfileLookupResult(List.of(), 0);
    }
    String lang = normalizeLang(language);
    String normalizedQuery = normalize(query);
    Set<String> terms = queryTerms(query);
    List<ScoredSnippet> scored = new ArrayList<>();
    JsonNode facts = root.path("facts");
    if (!facts.isArray()) {
      return new ProfileLookupResult(List.of(), 0);
    }
    int order = 0;
    for (JsonNode fact : facts) {
      String title = localized(fact.path("title"), lang);
      String text = localized(fact.path("text"), lang);
      List<String> tags = tags(fact.path("tags"));
      int factScore = score(normalizedQuery, terms, title, text, tags);
      if (factScore > 0) {
        scored.add(new ScoredSnippet(
            new RealtimeLookupSnippet("profile", title, text), factScore, order));
      }
      order++;
    }
    int bestScore = scored.stream().mapToInt(ScoredSnippet::score).max().orElse(0);
    List<RealtimeLookupSnippet> snippets = scored.stream()
        .filter(s -> s.score() >= MIN_RELEVANCE_SCORE)
        .sorted(Comparator.comparingInt(ScoredSnippet::score).reversed()
            .thenComparingInt(ScoredSnippet::order))
        .map(ScoredSnippet::snippet)
        .limit(Math.max(1, maxResults))
        .toList();
    return new ProfileLookupResult(snippets, bestScore);
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
      if (fieldMatches(titleNorm, normalizedQuery)) {
        score += 12;
      }
      if (tagNorms.stream().anyMatch(t -> fieldMatches(t, normalizedQuery))) {
        score += 10;
      }
      if (fieldMatches(textNorm, normalizedQuery)) {
        score += 6;
      }
    }
    for (String term : terms) {
      if (fieldMatches(titleNorm, term)) {
        score += 4;
      }
      if (tagNorms.stream().anyMatch(t -> fieldMatches(t, term))) {
        score += 3;
      }
      if (fieldMatches(textNorm, term)) {
        score += 1;
      }
    }
    return score;
  }

  /** Substring match with light fuzzy tolerance for voice transcription typos. */
  static boolean fieldMatches(String fieldNorm, String needle) {
    if (!StringUtils.hasText(needle) || !StringUtils.hasText(fieldNorm)) {
      return false;
    }
    if (fieldNorm.contains(needle)) {
      return true;
    }
    if (needle.length() < 3) {
      return false;
    }
    int maxDistance = needle.length() <= 5 ? 1 : 2;
    for (String part : fieldNorm.split("[^\\p{L}\\p{N}]+")) {
      if (part.length() < 2) {
        continue;
      }
      if (levenshteinDistance(part, needle) <= maxDistance) {
        return true;
      }
    }
    return false;
  }

  static int levenshteinDistance(String a, String b) {
    if (a.equals(b)) {
      return 0;
    }
    int[] prev = new int[b.length() + 1];
    int[] curr = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      curr[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      int[] swap = prev;
      prev = curr;
      curr = swap;
    }
    return prev[b.length()];
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
