package com.kevinmazali.portfolio.service;

import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Bilingual canned redirects when a query is off-topic or retrieval finds no relevant context.
 */
@Component
public class OffTopicRedirectMessages {

  private static final String EN_OFF_TOPIC =
      "I don't have information about that. I can tell you about Kevin's studies in data engineering "
          + "at NTNU, his projects such as this portfolio site and Krisefikser, or his technical skills "
          + "and experience. What would you like to know?";

  private static final String NO_OFF_TOPIC =
      "Jeg har ikke informasjon om det. Jeg kan fortelle om Kevin sine studier i dataingeniør ved NTNU, "
          + "prosjektene hans som denne porteføljesiden og Krisefikser, eller tekniske ferdigheter og "
          + "erfaring. Hva vil du vite?";

  private static final String EN_NO_CONTEXT =
      "The information I have about Kevin doesn't include that. Try asking about his studies at NTNU, "
          + "his projects, or his experience.";

  private static final String NO_NO_CONTEXT =
      "Det står ikke i informasjonen jeg har om Kevin. Prøv å spørre om studiene hans ved NTNU, "
          + "prosjektene hans eller erfaringen hans.";

  public String offTopicRedirect(String rawQuery) {
    return isNorwegian(rawQuery) ? NO_OFF_TOPIC : EN_OFF_TOPIC;
  }

  public String noRelevantContext(String rawQuery) {
    return isNorwegian(rawQuery) ? NO_NO_CONTEXT : EN_NO_CONTEXT;
  }

  /**
   * Heuristic language detection for redirect copy (matches user question language).
   */
  static boolean isNorwegian(String raw) {
    if (!StringUtils.hasText(raw)) {
      return false;
    }
    String q = raw.toLowerCase(Locale.ROOT);
    if (q.matches(".*[æøå].*")) {
      return true;
    }
    return q.contains(" hva ")
        || q.contains(" hvordan ")
        || q.contains(" hvor ")
        || q.contains(" kan du ")
        || q.contains(" fortell ")
        || q.contains(" studerer ")
        || q.contains(" prosjekt")
        || q.contains(" erfaring")
        || q.contains(" karakter")
        || q.contains(" ntnu")
        || q.startsWith("hva ")
        || q.startsWith("hvordan ")
        || q.startsWith("fortell ");
  }
}
