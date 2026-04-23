package com.kevinmazali.portfolio.model.experiment;

/** One eval dataset row for admin UI (stored in PostgreSQL). */
public record EvalDatasetSummary(String id, String name, int exampleCount) {}
