ALTER TABLE eval_datasets
    ADD COLUMN IF NOT EXISTS source VARCHAR(32) NOT NULL DEFAULT 'manual';

ALTER TABLE experiment_results
    ADD COLUMN IF NOT EXISTS eval_example_id BIGINT REFERENCES eval_dataset_examples (id);

CREATE INDEX IF NOT EXISTS idx_expres_eval_example ON experiment_results (eval_example_id);
