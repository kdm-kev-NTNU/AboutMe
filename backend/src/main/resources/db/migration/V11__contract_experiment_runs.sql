DROP VIEW IF EXISTS v_experiment_run_summary;

ALTER TABLE experiment_runs DROP COLUMN IF EXISTS dataset_name;
ALTER TABLE experiment_runs DROP COLUMN IF EXISTS mean_faithfulness;
ALTER TABLE experiment_runs DROP COLUMN IF EXISTS mean_relevance;
ALTER TABLE experiment_runs DROP COLUMN IF EXISTS mean_correctness;
ALTER TABLE experiment_runs DROP COLUMN IF EXISTS mean_conciseness;
ALTER TABLE experiment_runs DROP COLUMN IF EXISTS mean_language_consistency;

CREATE OR REPLACE VIEW v_experiment_run_summary AS
SELECT
    er.id,
    er.name,
    ed.name AS dataset_name,
    er.eval_dataset_id,
    er.generator_model,
    er.evaluator_model,
    er.status,
    er.total_examples,
    AVG(CASE WHEN ems.metric = 'faithfulness' THEN ems.score END) AS mean_faithfulness,
    AVG(CASE WHEN ems.metric = 'relevance' THEN ems.score END) AS mean_relevance,
    AVG(CASE WHEN ems.metric = 'correctness' THEN ems.score END) AS mean_correctness,
    AVG(CASE WHEN ems.metric = 'conciseness' THEN ems.score END) AS mean_conciseness,
    AVG(CASE WHEN ems.metric = 'language_consistency' THEN ems.score END) AS mean_language_consistency,
    er.error_message,
    er.created_at,
    er.completed_at
FROM experiment_runs er
LEFT JOIN eval_datasets ed ON ed.id = er.eval_dataset_id
LEFT JOIN experiment_results res ON res.experiment_run_id = er.id
LEFT JOIN experiment_metric_scores ems ON ems.experiment_result_id = res.id
GROUP BY er.id, ed.name;
