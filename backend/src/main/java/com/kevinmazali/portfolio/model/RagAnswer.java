package com.kevinmazali.portfolio.model;

import java.util.List;

/**
 * RAG output including retrieved chunk texts (for eval faithfulness / document relevance).
 */
public record RagAnswer(
    String answer,
    List<String> documentTexts
) {
}
