package com.kevinmazali.portfolio.model.experiment;

/**
 * Lifecycle state for an eval experiment run persisted in MySQL.
 */
public enum ExperimentRunStatus {
  RUNNING,
  COMPLETED,
  FAILED
}
