package com.kevinmazali.portfolio.model;

/**
 * Admin request to propose default chatbot starter questions from indexed chunks or an exported JSON snapshot.
 *
 * @param source {@code currentChunks} or {@code uploadedJson}
 * @param documentId optional filter when {@code source} is {@code currentChunks} (content hash / document_id)
 * @param chunksJson required when {@code source} is {@code uploadedJson}; must contain a top-level {@code chunks} array
 * @param model chat model id ({@link com.kevinmazali.portfolio.model.chat.SupportedChatModel})
 * @param maxQuestions clamped between 3 and 30
 * @param language human-readable instruction for question language (e.g. Norwegian, English)
 */
public record DefaultQuestionSuggestionRequest(
    String source,
    String documentId,
    String chunksJson,
    String model,
    Integer maxQuestions,
    String language
) {}
