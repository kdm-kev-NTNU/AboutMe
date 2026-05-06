package com.kevinmazali.portfolio.model;

import java.util.List;

/** LLM-proposed short starter questions for the public chat UI. */
public record DefaultQuestionSuggestionResponse(List<String> suggestions, String modelUsed) {}
