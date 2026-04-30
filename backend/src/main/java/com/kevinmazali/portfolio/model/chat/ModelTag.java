package com.kevinmazali.portfolio.model.chat;

/**
 * High-level capability / latency category for chat models exposed in the catalog.
 */
public enum ModelTag {
  /** Lower-latency, cost-efficient models suitable as defaults. */
  FAST,
  /** Higher-capability models; may require authentication for anonymous public chat. */
  REASONING
}
