package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.experiment.GeneratedQaItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of Piscada {@code postprocess.py} + {@code text_similarity.jaccard_word_set} for QRA dataset generation.
 */
public final class QaPostprocessor {

  private static final Pattern QUESTION_PREFIX = Pattern.compile("^(Question|Q):\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern ANSWER_PREFIX = Pattern.compile("^(Answer|A):\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern TRAILING_PARENS = Pattern.compile("\\s*\\(.*?\\)\\s*$");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private static final String[] ARTIFACTS = {
    "requirements:", "note:", "source:", "text:", "focus area:"
  };

  private static final String[] QUESTION_WORDS = {
    "what", "how", "why", "when", "where", "who", "which", "does", "is", "are", "can"
  };

  private QaPostprocessor() {}

  public static String[] cleanQaItem(String question, String answer) {
    String q = question == null ? "" : question.strip();
    String a = answer == null ? "" : answer.strip();
    q = QUESTION_PREFIX.matcher(q).replaceFirst("");
    a = ANSWER_PREFIX.matcher(a).replaceFirst("");
    q = TRAILING_PARENS.matcher(q).replaceFirst("");
    q = WHITESPACE.matcher(q).replaceAll(" ").strip();
    a = WHITESPACE.matcher(a).replaceAll(" ").strip();
    return new String[] {q, a};
  }

  public static boolean isLowQuality(String question, String answer) {
    return isLowQuality(question, answer, 10);
  }

  public static boolean isLowQuality(String question, String answer, int minQuestionLen) {
    if (question == null || answer == null) {
      return true;
    }
    if (question.length() < minQuestionLen || answer.length() < 5) {
      return true;
    }
    String ql = question.toLowerCase(Locale.ROOT);
    String al = answer.toLowerCase(Locale.ROOT);
    for (String artifact : ARTIFACTS) {
      if (ql.contains(artifact) || al.contains(artifact)) {
        return true;
      }
    }
    boolean hasQuestionWord = false;
    for (String w : QUESTION_WORDS) {
      if (ql.contains(w)) {
        hasQuestionWord = true;
        break;
      }
    }
    boolean hasQuestionMark = question.contains("?");
    return !(hasQuestionWord || hasQuestionMark);
  }

  /** Jaccard similarity on whitespace-split word sets (Piscada {@code jaccard_word_set}). */
  public static double jaccardWordSet(String a, String b) {
    Set<String> words1 = wordSetSplit(a);
    Set<String> words2 = wordSetSplit(b);
    if (words1.isEmpty() || words2.isEmpty()) {
      return 0.0;
    }
    Set<String> intersection = new HashSet<>(words1);
    intersection.retainAll(words2);
    Set<String> union = new HashSet<>(words1);
    union.addAll(words2);
    return (double) intersection.size() / (double) union.size();
  }

  private static Set<String> wordSetSplit(String s) {
    Set<String> out = new HashSet<>();
    if (s == null || s.isBlank()) {
      return out;
    }
    for (String part : s.toLowerCase(Locale.ROOT).split("\\s+")) {
      if (!part.isEmpty()) {
        out.add(part);
      }
    }
    return out;
  }

  public static List<GeneratedQaItem> deduplicateByJaccard(List<GeneratedQaItem> items, double similarityThreshold) {
    List<GeneratedQaItem> kept = new ArrayList<>();
    List<String> seenQuestions = new ArrayList<>();
    for (GeneratedQaItem item : items) {
      String question = item.question();
      if (question == null || question.isBlank()) {
        continue;
      }
      boolean duplicate = false;
      for (String seen : seenQuestions) {
        if (jaccardWordSet(question, seen) >= similarityThreshold) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        kept.add(item);
        seenQuestions.add(question);
      }
    }
    return kept;
  }
}
