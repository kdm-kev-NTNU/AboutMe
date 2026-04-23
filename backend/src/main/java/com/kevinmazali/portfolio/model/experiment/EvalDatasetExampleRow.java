package com.kevinmazali.portfolio.model.experiment;

/** One example row for RAG eval runs (question + optional gold reference). */
public record EvalDatasetExampleRow(String question, String referenceText) {}
