package com.kevinmazali.portfolio.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseCleanerTest {

    private final NoiseCleaner cleaner = new NoiseCleaner();

    @Test
    void cleanNoiseReturnsNullForNull() {
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(null);
        assertEquals(null, result.cleanedText());
    }

    @Test
    void cleanNoiseReturnsEmptyForEmpty() {
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise("");
        assertEquals("", result.cleanedText());
    }

    @Test
    void removesPageNumbers() {
        String input = "Some content\nPage 1 of 5\nMore content\nPage 2\n42";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("Page 1 of 5"));
        assertFalse(result.cleanedText().contains("Page 2"));
        assertTrue(result.pageNumbersRemoved() >= 2);
    }

    @Test
    void removesNorwegianPageNumbers() {
        String input = "Innhold her\nSide 3 av 10\nMer innhold";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("Side 3 av 10"));
    }

    @Test
    void removesDashPageNumbers() {
        String input = "Content\n- 42 -\nMore";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("- 42 -"));
    }

    @Test
    void removesTocEntries() {
        String input = "Executive Summary .............. iv\nIntroduction ...................... 1\nActual content here";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("Executive Summary"));
        assertFalse(result.cleanedText().contains("Introduction ...."));
        assertTrue(result.cleanedText().contains("Actual content here"));
        assertTrue(result.tocLinesRemoved() >= 2);
    }

    @Test
    void removesFigureAndTableLabels() {
        String input = "Real text\nFigure 1. Some caption\nTable 3.\nFig. 2 Blah\nMore real text";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("Figure 1"));
        assertFalse(result.cleanedText().contains("Table 3"));
        assertFalse(result.cleanedText().contains("Fig. 2"));
        assertTrue(result.cleanedText().contains("Real text"));
        assertTrue(result.cleanedText().contains("More real text"));
    }

    @Test
    void removesListOfFiguresHeaders() {
        String input = "Content\nList of figures\nList of tables\nMore content";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("List of figures"));
        assertFalse(result.cleanedText().contains("List of tables"));
    }

    @Test
    void removesUrls() {
        String input = "Visit https://example.com/path for info\nAlso www.test.org and ftp://files.net/doc";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("https://"));
        assertFalse(result.cleanedText().contains("www.test"));
        assertFalse(result.cleanedText().contains("ftp://"));
        assertEquals(3, result.urlsRemoved());
    }

    @Test
    void sanitizesReplacementChars() {
        String input = "Hello\uFFFDworld\u200Bfoo";
        String result = cleaner.sanitizeReplacementChars(input);
        assertEquals("Helloworldfoo", result);
    }

    @Test
    void normalizesDecorativeLines() {
        String input = "Content\n............\n-----------\n___________\n***********\nMore content";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("............"));
        assertFalse(result.cleanedText().contains("-----------"));
        assertFalse(result.cleanedText().contains("___________"));
        assertFalse(result.cleanedText().contains("***********"));
        assertTrue(result.cleanedText().contains("Content"));
        assertTrue(result.cleanedText().contains("More content"));
    }

    @Test
    void collapsesMultipleBlankLines() {
        String input = "A\n\n\n\n\nB";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertFalse(result.cleanedText().contains("\n\n\n"));
        assertTrue(result.cleanedText().contains("A"));
        assertTrue(result.cleanedText().contains("B"));
    }

    @Test
    void preservesNormalContent() {
        String input = "This is a normal paragraph about Spring Boot.\nIt discusses dependency injection and REST APIs.";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertEquals(input, result.cleanedText());
        assertEquals(0, result.pageNumbersRemoved());
        assertEquals(0, result.tocLinesRemoved());
        assertEquals(0, result.urlsRemoved());
    }

    @Test
    void statsTrackOriginalAndCleanedLength() {
        String input = "Keep this\nPage 1 of 2\nhttps://remove.me\nKeep this too";
        NoiseCleaner.NoiseCleaningResult result = cleaner.cleanNoise(input);
        assertTrue(result.originalLength() > result.cleanedLength());
        assertTrue(result.cleanedLength() > 0);
    }
}
