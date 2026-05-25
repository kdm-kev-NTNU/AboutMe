ALTER TABLE experiment_runs VALIDATE CONSTRAINT fk_er_eval_dataset;
ALTER TABLE dataset_generations VALIDATE CONSTRAINT fk_dg_result_dataset;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pv_one_active_per_template
    ON prompt_versions (template_id)
    WHERE is_active = true AND template_id IS NOT NULL;
