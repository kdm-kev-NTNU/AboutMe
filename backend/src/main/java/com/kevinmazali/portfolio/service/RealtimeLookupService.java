package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.RealtimeLookupResponse;
import com.kevinmazali.portfolio.model.RealtimeLookupSnippet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Fast, voice-specific lookup: structured public facts first, small pgvector snippet fallback only.
 */
@Slf4j
@Service
public class RealtimeLookupService {

  private static final int MAX_QUERY_CHARS = 300;
  private static final int MAX_SNIPPETS = 5;
  private static final int VECTOR_TOP_K = 5;
  private static final int MAX_SNIPPET_CHARS = 500;
  private static final long CACHE_TTL_NANOS = Duration.ofSeconds(60).toNanos();
  /** Profile scores at or above this are treated as high-confidence matches. */
  private static final int HIGH_CONFIDENCE_PROFILE_SCORE = 6;
  /** Snippets shorter than this with only weak matches are low-confidence. */
  private static final int SHORT_SNIPPET_CHARS = 20;

  private final RealtimeProfileService profileService;
  private final VectorStore vectorStore;
  private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public RealtimeLookupService(RealtimeProfileService profileService, @Lazy VectorStore vectorStore) {
    this.profileService = profileService;
    this.vectorStore = vectorStore;
  }

  public RealtimeLookupResponse lookup(String rawQuery, String rawLanguage) {
    String query = validateQuery(rawQuery);
    String language = RealtimeProfileService.normalizeLang(rawLanguage);
    String cacheKey = language + ":" + query.toLowerCase(Locale.ROOT);
    long now = System.nanoTime();
    CacheEntry cached = cache.get(cacheKey);
    if (cached != null && now - cached.createdAtNanos() < CACHE_TTL_NANOS) {
      return cached.response();
    }

    RealtimeProfileService.ProfileLookupResult profileLookup =
        profileService.lookupDetailed(query, language, MAX_SNIPPETS);
    List<RealtimeLookupSnippet> snippets = new ArrayList<>(profileLookup.snippets());
    List<RealtimeLookupSnippet> vectorResults = List.of();
    if (snippets.size() < 2) {
      vectorResults = vectorSnippets(query, MAX_SNIPPETS - snippets.size());
      snippets.addAll(vectorResults);
    }

    List<RealtimeLookupSnippet> resultSnippets = dedupe(snippets).stream()
        .limit(MAX_SNIPPETS)
        .toList();
    String confidence = computeConfidence(
        resultSnippets, profileLookup.bestScore(), !vectorResults.isEmpty());
    RealtimeLookupResponse response =
        new RealtimeLookupResponse(!resultSnippets.isEmpty(), resultSnippets, confidence);
    cache.put(cacheKey, new CacheEntry(now, response));
    return response;
  }

  static String computeConfidence(
      List<RealtimeLookupSnippet> snippets, int bestProfileScore, boolean hasVectorResults) {
    if (snippets.isEmpty()) {
      return "none";
    }
    if (hasVectorResults || bestProfileScore >= HIGH_CONFIDENCE_PROFILE_SCORE) {
      return "high";
    }
    if (snippets.size() == 1 && snippets.get(0).text().length() < SHORT_SNIPPET_CHARS) {
      return "low";
    }
    if (bestProfileScore >= RealtimeProfileService.MIN_RELEVANCE_SCORE) {
      return "low";
    }
    return "low";
  }

  private static String validateQuery(String rawQuery) {
    if (!StringUtils.hasText(rawQuery)) {
      throw new IllegalArgumentException("Lookup query is required.");
    }
    String query = rawQuery.trim();
    if (query.length() > MAX_QUERY_CHARS) {
      throw new IllegalArgumentException(
          "Lookup query must be at most " + MAX_QUERY_CHARS + " characters.");
    }
    return query;
  }

  private List<RealtimeLookupSnippet> vectorSnippets(String query, int remaining) {
    if (remaining <= 0) {
      return List.of();
    }
    try {
      FilterExpressionBuilder b = new FilterExpressionBuilder();
      SearchRequest request = SearchRequest.builder()
          .query(query)
          .topK(VECTOR_TOP_K)
          .filterExpression(b.eq("content_type", "text").build())
          .build();
      return vectorStore.similaritySearch(request).stream()
          .filter(doc -> StringUtils.hasText(doc.getText()))
          .map(this::toRagSnippet)
          .limit(remaining)
          .toList();
    } catch (Exception e) {
      log.warn("Realtime voice vector lookup failed: {}", e.getMessage());
      return List.of();
    }
  }

  private RealtimeLookupSnippet toRagSnippet(Document document) {
    Object filename = document.getMetadata() == null ? null : document.getMetadata().get("filename");
    String title = filename == null || !StringUtils.hasText(String.valueOf(filename))
        ? "Retrieved document"
        : String.valueOf(filename);
    return new RealtimeLookupSnippet("rag", title, truncate(document.getText(), MAX_SNIPPET_CHARS));
  }

  private static List<RealtimeLookupSnippet> dedupe(List<RealtimeLookupSnippet> snippets) {
    Set<String> seen = new LinkedHashSet<>();
    List<RealtimeLookupSnippet> result = new ArrayList<>();
    for (RealtimeLookupSnippet snippet : snippets) {
      String key = snippet.sourceType() + "\n" + snippet.title() + "\n" + snippet.text();
      if (seen.add(key)) {
        result.add(snippet);
      }
    }
    return result;
  }

  private static String truncate(String text, int maxChars) {
    if (text == null) {
      return "";
    }
    String trimmed = text.trim();
    if (trimmed.length() <= maxChars) {
      return trimmed;
    }
    return trimmed.substring(0, maxChars);
  }

  private record CacheEntry(long createdAtNanos, RealtimeLookupResponse response) {}
}
