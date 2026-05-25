CREATE TABLE IF NOT EXISTS experiment_metric_scores (
    id BIGSERIAL PRIMARY KEY,
    experiment_result_id BIGINT NOT NULL REFERENCES experiment_results (id) ON DELETE CASCADE,
    metric VARCHAR(64) NOT NULL,
    score DOUBLE PRECISION,
    explanation TEXT
);

-- V1 may have created experiment_metric_scores without this constraint; ensure it exists for V10 ON CONFLICT.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_exp_metric_per_result'
          AND conrelid = 'public.experiment_metric_scores'::regclass
    ) THEN
        ALTER TABLE experiment_metric_scores
            ADD CONSTRAINT uq_exp_metric_per_result UNIQUE (experiment_result_id, metric);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_exp_metric_result ON experiment_metric_scores (experiment_result_id);
