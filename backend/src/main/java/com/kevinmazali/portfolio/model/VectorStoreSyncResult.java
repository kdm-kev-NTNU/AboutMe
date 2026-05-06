package com.kevinmazali.portfolio.model;

/**
 * Result of {@code POST /admin/tools/documents/sync-from-remote}.
 */
public record VectorStoreSyncResult(
    long rowsSynced,
    long durationMs,
    /** Host/port/database parsed from JDBC URL (no credentials). */
    String sourceHostMasked,
    /** Whether the local table was truncated before upsert. */
    boolean truncatedLocalFirst
) {}
