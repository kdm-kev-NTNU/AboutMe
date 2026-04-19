package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.SanitizeResult;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII detection and redaction using regex patterns (email, phone)
 * and Apache OpenNLP NER (person names).
 * <p>
 * Replaces each detected span with a redaction marker
 * such as {@code {{{REDACTED-email-address}}}}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "portfolio.sanitizer.enabled", havingValue = "true", matchIfMissing = true)
public class PiiSanitizerService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+?1[\\s.-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}");

    private final TokenNameFinderModel nameModel;
    private final TokenizerModel tokenizerModel;

    public PiiSanitizerService(TokenNameFinderModel nameModel, TokenizerModel tokenizerModel) {
        this.nameModel = nameModel;
        this.tokenizerModel = tokenizerModel;
        log.info("PII sanitizer initialized (regex + OpenNLP NER)");
    }

    /**
     * Detects and redacts PII in the given text.
     *
     * @return result with sanitized text and detection stats
     */
    public SanitizeResult sanitize(String text) {
        if (text == null || text.isBlank()) {
            return new SanitizeResult(text, 0, Map.of(), "PASS");
        }

        try {
            List<PiiMatch> matches = new ArrayList<>();
            collectRegexMatches(text, matches);
            collectNameMatches(text, matches);

            matches.sort((a, b) -> Integer.compare(b.start(), a.start()));
            matches = deduplicateOverlapping(matches);

            String redacted = text;
            for (PiiMatch match : matches) {
                String replacement = "{{{REDACTED-" + match.type() + "}}}";
                redacted = redacted.substring(0, match.start()) + replacement + redacted.substring(match.end());
            }

            int detected = matches.size();
            Map<String, Integer> typeCount = new HashMap<>();
            for (PiiMatch match : matches) {
                typeCount.merge(match.type(), 1, Integer::sum);
            }

            String status = detected == 0 ? "PASS" : "REVIEW";
            log.debug("PII sanitization: {} entities detected ({})", detected, typeCount);
            return new SanitizeResult(redacted, detected, typeCount, status);
        } catch (Exception e) {
            log.warn("PII sanitization failed, returning original text: {}", e.getMessage());
            return new SanitizeResult(text, 0, Map.of(), "FAIL");
        }
    }

    private void collectRegexMatches(String text, List<PiiMatch> matches) {
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            matches.add(new PiiMatch(emailMatcher.start(), emailMatcher.end(), "email-address"));
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            matches.add(new PiiMatch(phoneMatcher.start(), phoneMatcher.end(), "phone-number"));
        }
    }

    private void collectNameMatches(String text, List<PiiMatch> matches) {
        TokenizerME tokenizer = new TokenizerME(tokenizerModel);
        NameFinderME nameFinder = new NameFinderME(nameModel);

        String[] tokens = tokenizer.tokenize(text);
        Span[] tokenSpans = tokenizer.tokenizePos(text);
        Span[] nameSpans = nameFinder.find(tokens);

        for (Span nameSpan : nameSpans) {
            int charStart = tokenSpans[nameSpan.getStart()].getStart();
            int charEnd = tokenSpans[nameSpan.getEnd() - 1].getEnd();
            matches.add(new PiiMatch(charStart, charEnd, "person-name"));
        }

        nameFinder.clearAdaptiveData();
    }

    /**
     * Removes overlapping matches, keeping the one that starts later
     * (list is already sorted descending by start position).
     */
    private List<PiiMatch> deduplicateOverlapping(List<PiiMatch> sorted) {
        List<PiiMatch> result = new ArrayList<>();
        int lastStart = Integer.MAX_VALUE;
        for (PiiMatch m : sorted) {
            if (m.end() <= lastStart) {
                result.add(m);
                lastStart = m.start();
            }
        }
        return result;
    }

    private record PiiMatch(int start, int end, String type) {}
}
