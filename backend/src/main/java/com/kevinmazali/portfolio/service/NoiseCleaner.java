package com.kevinmazali.portfolio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based text noise removal inspired by Piscada's document pipeline.
 * Removes page numbers, TOC entries, figure/table labels, URLs,
 * Unicode replacement characters, and decorative whitespace.
 */
@Slf4j
@Component
public class NoiseCleaner {

    private static final Pattern PAGE_NUM_FULL = Pattern.compile(
            "^Page\\s+\\d+\\s+of\\s+\\d+\\s*$", Pattern.MULTILINE);
    private static final Pattern PAGE_NUM_SHORT = Pattern.compile(
            "^Page\\s+\\d+\\s*$", Pattern.MULTILINE);
    private static final Pattern PAGE_NUM_NORWEGIAN = Pattern.compile(
            "^Side\\s+\\d+\\s+av\\s+\\d+\\s*$", Pattern.MULTILINE);
    private static final Pattern PAGE_NUM_DASH = Pattern.compile(
            "^-\\s*\\d+\\s*-\\s*$", Pattern.MULTILINE);
    private static final Pattern PAGE_NUM_BARE = Pattern.compile(
            "^\\d+\\s*$", Pattern.MULTILINE);

    private static final List<Pattern> PAGE_NUMBER_PATTERNS = List.of(
            PAGE_NUM_FULL, PAGE_NUM_SHORT, PAGE_NUM_NORWEGIAN, PAGE_NUM_DASH, PAGE_NUM_BARE);

    private static final Pattern TOC_ENTRY = Pattern.compile(
            "^.+[.\\u2026\\-_]{3,}\\s*(?:\\d{1,4}|[ivxlc]{1,5})\\s*$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern FIGURE_TABLE_LINE = Pattern.compile(
            "^(?:Figure\\s+\\d+|Fig\\.?\\s*\\d+|Table\\s+\\d+|Tab\\.?\\s*\\d+|Box\\s+\\d+)\\.?\\s*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://\\S+|www\\.\\S+|ftp://\\S+");

    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n\\n\\n+");

    private static final List<Pattern> WHITESPACE_LINE_PATTERNS = List.of(
            Pattern.compile("^\\s*\\.+\\s*$"),
            Pattern.compile("^\\s*-+\\s*$"),
            Pattern.compile("^\\s*_+\\s*$"),
            Pattern.compile("^\\s*\\*+\\s*$"));

    private static final Set<String> LIST_OF_ARTIFACTS_PREFIXES = Set.of(
            "list of figures", "list of tables", "list of boxes");

    private static final char REPLACEMENT_CHAR = '\uFFFD';
    private static final char ZERO_WIDTH_SPACE = '\u200B';

    public NoiseCleaningResult cleanNoise(String text) {
        if (text == null || text.isEmpty()) {
            return new NoiseCleaningResult(text, 0, 0, 0, 0, 0, 0, 0);
        }

        int originalLength = text.length();

        text = sanitizeReplacementChars(text);

        int pageNumbersRemoved = 0;
        for (Pattern p : PAGE_NUMBER_PATTERNS) {
            int before = countMatches(p, text);
            pageNumbersRemoved += before;
            text = p.matcher(text).replaceAll("");
        }

        int tocLinesRemoved = countMatches(TOC_ENTRY, text);
        text = TOC_ENTRY.matcher(text).replaceAll("");

        RemoveResult ftResult = removeFigureTableLines(text);
        text = ftResult.text;

        int urlsRemoved = countMatches(URL_PATTERN, text);
        text = URL_PATTERN.matcher(text).replaceAll("");

        RemoveResult wsResult = normalizeWhitespace(text);
        text = wsResult.text;

        int charsRemoved = originalLength - text.length();

        log.debug("Noise cleaning: removed {} page numbers, {} TOC lines, {} fig/table lines, "
                        + "{} URLs, {} whitespace lines ({} chars total)",
                pageNumbersRemoved, tocLinesRemoved, ftResult.count,
                urlsRemoved, wsResult.count, charsRemoved);

        return new NoiseCleaningResult(text, pageNumbersRemoved, tocLinesRemoved,
                ftResult.count, urlsRemoved, wsResult.count, originalLength, text.length());
    }

    String sanitizeReplacementChars(String text) {
        if (text.indexOf(REPLACEMENT_CHAR) < 0 && text.indexOf(ZERO_WIDTH_SPACE) < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != REPLACEMENT_CHAR && c != ZERO_WIDTH_SPACE) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    RemoveResult removeFigureTableLines(String text) {
        String[] lines = text.split("\n", -1);
        List<String> cleaned = new ArrayList<>(lines.length);
        int removed = 0;
        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) {
                cleaned.add(line);
                continue;
            }
            String lower = stripped.toLowerCase();
            if (LIST_OF_ARTIFACTS_PREFIXES.stream().anyMatch(p -> lower.equals(p) || lower.startsWith(p))) {
                removed++;
                continue;
            }
            if (FIGURE_TABLE_LINE.matcher(stripped).find()) {
                removed++;
                continue;
            }
            cleaned.add(line);
        }
        return new RemoveResult(String.join("\n", cleaned), removed);
    }

    RemoveResult normalizeWhitespace(String text) {
        String[] lines = text.split("\n", -1);
        List<String> cleaned = new ArrayList<>(lines.length);
        int removed = 0;
        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) {
                removed++;
                continue;
            }
            boolean decorative = false;
            for (Pattern p : WHITESPACE_LINE_PATTERNS) {
                if (p.matcher(stripped).matches()) {
                    decorative = true;
                    break;
                }
            }
            if (decorative) {
                removed++;
                continue;
            }
            cleaned.add(line);
        }
        String result = String.join("\n", cleaned);
        result = MULTI_NEWLINE.matcher(result).replaceAll("\n\n");
        return new RemoveResult(result, removed);
    }

    private static int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    record RemoveResult(String text, int count) {}

    public record NoiseCleaningResult(
            String cleanedText,
            int pageNumbersRemoved,
            int tocLinesRemoved,
            int figureTableLinesRemoved,
            int urlsRemoved,
            int whitespaceLinesRemoved,
            int originalLength,
            int cleanedLength
    ) {}
}
