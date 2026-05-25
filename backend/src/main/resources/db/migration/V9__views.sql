CREATE OR REPLACE VIEW v_experiment_run_summary AS
SELECT
    er.id,
    er.name,
    COALESCE(ed.name, er.dataset_name) AS dataset_name,
    er.eval_dataset_id,
    er.generator_model,
    er.evaluator_model,
    er.status,
    er.total_examples,
    er.mean_faithfulness,
    er.mean_relevance,
    er.mean_correctness,
    er.mean_conciseness,
    er.mean_language_consistency,
    er.error_message,
    er.created_at,
    er.completed_at
FROM experiment_runs er
LEFT JOIN eval_datasets ed ON ed.id = er.eval_dataset_id;

CREATE OR REPLACE VIEW v_ai_usage_daily AS
SELECT
    identity_type,
    identity_key,
    user_id,
    model,
    DATE(created_at AT TIME ZONE 'UTC') AS usage_day,
    SUM(prompt_tokens) AS prompt_tokens,
    SUM(completion_tokens) AS completion_tokens,
    SUM(estimated_cost_usd) AS estimated_cost_usd
FROM ai_usage_events
GROUP BY identity_type, identity_key, user_id, model, DATE(created_at AT TIME ZONE 'UTC');
