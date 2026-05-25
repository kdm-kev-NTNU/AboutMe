CREATE TABLE IF NOT EXISTS experiment_metric_scores (
    id BIGSERIAL PRIMARY KEY,
    experiment_result_id BIGINT NOT NULL REFERENCES experiment_results (id) ON DELETE CASCADE,
    metric VARCHAR(64) NOT NULL,
    score DOUBLE PRECISION,
    explanation TEXT,
    CONSTRAINT uq_exp_metric_per_result UNIQUE (experiment_result_id, metric)
);

CREATE INDEX IF NOT EXISTS idx_exp_metric_result ON experiment_metric_scores (experiment_result_id);
