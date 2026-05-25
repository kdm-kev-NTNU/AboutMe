-- Pre-migration audit: run before Flyway V2+ on staging/prod copy.

SELECT 'row_counts' AS section;
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'ai_usage_record', COUNT(*) FROM ai_usage_record
UNION ALL SELECT 'prompt_versions', COUNT(*) FROM prompt_versions
UNION ALL SELECT 'experiment_runs', COUNT(*) FROM experiment_runs
UNION ALL SELECT 'experiment_results', COUNT(*) FROM experiment_results
UNION ALL SELECT 'eval_datasets', COUNT(*) FROM eval_datasets
UNION ALL SELECT 'dataset_generations', COUNT(*) FROM dataset_generations
UNION ALL SELECT 'request_log', COUNT(*) FROM request_log;

SELECT 'orphan_experiment_runs' AS section, COUNT(*) AS cnt
FROM experiment_runs er
WHERE er.eval_dataset_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eval_datasets d WHERE d.id = er.eval_dataset_id);

SELECT 'null_eval_dataset_id_runs' AS section, COUNT(*) AS cnt
FROM experiment_runs WHERE eval_dataset_id IS NULL;

SELECT 'orphan_dataset_generations' AS section, COUNT(*) AS cnt
FROM dataset_generations dg
WHERE dg.result_dataset_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eval_datasets d WHERE d.id = dg.result_dataset_id);

SELECT 'duplicate_active_prompt_variants' AS section, name, language, provider, COUNT(*) AS active_cnt
FROM prompt_versions WHERE is_active = true
GROUP BY name, language, provider
HAVING COUNT(*) > 1;
