package com.kevinmazali.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kevinmazali.portfolio.model.experiment.GeneratedQaItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class QaPostprocessorTest {

  @Test
  void cleanQaItemStripsPrefixesAndWhitespace() {
    String[] out = QaPostprocessor.cleanQaItem("  Question: What is X?  ", "  Answer:  Y  ");
    assertEquals("What is X?", out[0]);
    assertEquals("Y", out[1]);
  }

  @Test
  void isLowQualityRejectsShortOrNonQuestions() {
    assertTrue(QaPostprocessor.isLowQuality("short", "long enough answer here"));
    assertTrue(QaPostprocessor.isLowQuality("Alpha bravo charlie delta echo.", "answer"));
    assertFalse(QaPostprocessor.isLowQuality("What is the capital of France?", "Paris."));
  }

  @Test
  void jaccardWordSetMatchesPiscadaStyleOverlap() {
    double sim = QaPostprocessor.jaccardWordSet("what is foo bar", "what is foo baz");
    assertTrue(sim > 0.5);
    assertEquals(0.0, QaPostprocessor.jaccardWordSet("", "a b"), 0.0001);
  }

  @Test
  void deduplicateByJaccardRemovesNearDuplicates() {
    String nearDup = "What is foo bar baz qux corge waldo fred plugh xyzzy thud?";
    List<GeneratedQaItem> items =
        List.of(
            new GeneratedQaItem(nearDup, "A", "f", "d", 0),
            new GeneratedQaItem(nearDup + " extra", "B", "f", "d", 1),
            new GeneratedQaItem("How does gravity work?", "C", "f", "d", 2));
    List<GeneratedQaItem> kept = QaPostprocessor.deduplicateByJaccard(items, 0.85);
    assertEquals(2, kept.size());
    assertTrue(kept.stream().anyMatch(i -> i.question().contains("gravity")));
  }
}
