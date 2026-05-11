package com.kevinmazali.portfolio.model;

/**
 * Public voice provider/model option exposed to the SPA.
 */
public record RealtimeModelOption(
    String provider,
    String id,
    String label,
    boolean defaultOption) {}
