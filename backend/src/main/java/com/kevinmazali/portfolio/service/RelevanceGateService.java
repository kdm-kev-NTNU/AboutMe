package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RelevanceGateProperties;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Lightweight pre-RAG gate: rejects obviously off-topic queries before vector search / LLM.
 * Prefers false negatives (let borderline questions through).
 */
@Service
public class RelevanceGateService {

  public enum Verdict {
    IN_SCOPE,
    OFF_TOPIC
  }

  private static final int SHORT_QUERY_MAX_SIGNIFICANT_TERMS = 2;

  private static final List<String> IN_SCOPE_KEYWORDS = List.of(
      "kevin", "portfolio", "portefølje", "portefolje", "ntnu", "data engineering", "dataingeniør",
      "dataingeniør", "studies", "studier", "study", "studerer", "student", "project", "prosjekter",
      "prosjekt", "experience", "erfaring", "course", "kurs", "fag", "subject", "spring", "rag",
      "chatbot", "website", "nettside", "skills", "ferdigheter", "programming", "programmering",
      "internship", "praksis", "work", "arbeid", "jobb", "employer", "arbeidsgiver", "lego",
      "krisefikser", "machine learning", "maskinlæring", "statistics", "statistikk", "agile",
      "software", "programvare", "developer", "utvikler", "engineer", "ingeniør", "cv", "resume",
      "github", "java", "python", "typescript", "vue", "react", "database", "postgres", "vector",
      "embedding", "openai", "anthropic", "whisper", "voice", "stemme", "realtime", "about me",
      "aboutme", "who is", "hvem er", "tell me about", "fortell om", "background", "bakgrunn",
      "education", "utdanning", "university", "universitet", "thesis", "oppgave", "grade",
      "karakter", "karakterer", "contact", "kontakt", "email", "e-post");

  private static final List<Pattern> OUT_OF_SCOPE_PATTERNS = List.of(
      Pattern.compile("meaning of life", Pattern.CASE_INSENSITIVE),
      Pattern.compile("meningen med livet", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bweather\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bvær\\b|\\bværet\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\brecipe\\b|\\boppskrift\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("world cup|vm i fotball|fotball-?vm", Pattern.CASE_INSENSITIVE),
      Pattern.compile("write (me )?a poem|skriv (meg )?et dikt", Pattern.CASE_INSENSITIVE),
      Pattern.compile("quantum physics|kvantefysikk", Pattern.CASE_INSENSITIVE),
      Pattern.compile("who won (the )?(world|super)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("homework|lekse|matteoppgave", Pattern.CASE_INSENSITIVE),
      Pattern.compile("capital of (?!ntnu)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("hva er hovedstaden", Pattern.CASE_INSENSITIVE),
      Pattern.compile("ignore (all |your )?instructions", Pattern.CASE_INSENSITIVE),
      Pattern.compile("forget (your |the )?rules", Pattern.CASE_INSENSITIVE),
      Pattern.compile("system prompt|reveal (your )?prompt", Pattern.CASE_INSENSITIVE));

  private static final List<Pattern> KEVIN_REFERENCE_PATTERNS = List.of(
      Pattern.compile("\\b(he|him|his)\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(han|hans|ham)\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(du|din|dine)\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(you|your)\\b", Pattern.CASE_INSENSITIVE));

  private final RelevanceGateProperties properties;

  public RelevanceGateService(RelevanceGateProperties properties) {
    this.properties = properties;
  }

  public Verdict evaluate(String rawQuery) {
    if (!properties.isEnabled() || !StringUtils.hasText(rawQuery)) {
      return Verdict.IN_SCOPE;
    }
    String normalized = normalize(rawQuery);
    if (normalized.isEmpty()) {
      return Verdict.IN_SCOPE;
    }

    int inScopeScore = scoreInScope(normalized);
    int outScopeScore = scoreOutOfScope(normalized);
    int significantTerms = countSignificantTerms(normalized);

    if (outScopeScore >= 1 && inScopeScore == 0) {
      return Verdict.OFF_TOPIC;
    }
    if (significantTerms <= SHORT_QUERY_MAX_SIGNIFICANT_TERMS) {
      return Verdict.IN_SCOPE;
    }
    if (inScopeScore >= 1) {
      return Verdict.IN_SCOPE;
    }
    if (outScopeScore >= 1) {
      return Verdict.OFF_TOPIC;
    }
    if (properties.isStrictMode() && normalized.length() > 30) {
      return Verdict.OFF_TOPIC;
    }
    return Verdict.IN_SCOPE;
  }

  private static int scoreInScope(String normalized) {
    int score = 0;
    for (String keyword : IN_SCOPE_KEYWORDS) {
      if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
        score++;
      }
    }
    for (Pattern p : KEVIN_REFERENCE_PATTERNS) {
      if (p.matcher(normalized).find()) {
        score += 2;
        break;
      }
    }
    return score;
  }

  private static int scoreOutOfScope(String normalized) {
    int score = 0;
    for (Pattern p : OUT_OF_SCOPE_PATTERNS) {
      if (p.matcher(normalized).find()) {
        score++;
      }
    }
    return score;
  }

  private static int countSignificantTerms(String normalized) {
    int count = 0;
    for (String part : normalized.split("[^\\p{L}\\p{N}]+")) {
      if (part.length() > 1 && !STOP_WORDS.contains(part)) {
        count++;
      }
    }
    return count;
  }

  private static final Set<String> STOP_WORDS = Set.of(
      "the", "and", "for", "with", "what", "does", "about", "tell", "his", "her", "him", "who",
      "how", "why", "when", "where", "is", "are", "was", "were", "can", "you", "me", "my", "your",
      "hva", "om", "og", "med", "kan", "du", "han", "hans", "som", "det", "den", "er", "var",
      "kevin");

  private static String normalize(String raw) {
    return raw.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
  }
}
