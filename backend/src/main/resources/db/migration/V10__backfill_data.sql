-- Data backfill (idempotent). Documents backfill runs only when vector_store exists.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'vector_store'
    ) THEN
        INSERT INTO documents (id, filename, content_hash, ingested_at)
        SELECT
            doc_id,
            COALESCE(MAX(filename), doc_id),
            doc_id,
            COALESCE(MAX(NULLIF(trim(meta_ingested), '')::timestamptz), now())
        FROM (
            SELECT
                metadata->>'document_id' AS doc_id,
                metadata->>'filename' AS filename,
                metadata->>'ingested_at' AS meta_ingested
            FROM vector_store
            WHERE COALESCE(metadata->>'document_id', '') <> ''
        ) sub
        GROUP BY doc_id
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;

INSERT INTO prompt_templates (name, language, provider)
SELECT DISTINCT name, language, provider FROM prompt_versions
ON CONFLICT (name, language, provider) DO NOTHING;

UPDATE prompt_versions pv
SET template_id = pt.id
FROM prompt_templates pt
WHERE pv.template_id IS NULL
  AND pv.name = pt.name
  AND (pv.language IS NOT DISTINCT FROM pt.language)
  AND (pv.provider IS NOT DISTINCT FROM pt.provider);

-- Legacy experiment_results stored metrics inline; V1 Flyway baseline uses experiment_metric_scores only.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'experiment_results'
          AND column_name = 'faithfulness'
    ) THEN
        INSERT INTO experiment_metric_scores (experiment_result_id, metric, score, explanation)
        SELECT id, 'faithfulness', faithfulness, faithfulness_explanation
        FROM experiment_results WHERE faithfulness IS NOT NULL
        ON CONFLICT (experiment_result_id, metric) DO NOTHING;

        INSERT INTO experiment_metric_scores (experiment_result_id, metric, score, explanation)
        SELECT id, 'relevance', relevance, relevance_explanation
        FROM experiment_results WHERE relevance IS NOT NULL
        ON CONFLICT (experiment_result_id, metric) DO NOTHING;

        INSERT INTO experiment_metric_scores (experiment_result_id, metric, score, explanation)
        SELECT id, 'correctness', correctness, correctness_explanation
        FROM experiment_results WHERE correctness IS NOT NULL
        ON CONFLICT (experiment_result_id, metric) DO NOTHING;

        INSERT INTO experiment_metric_scores (experiment_result_id, metric, score, explanation)
        SELECT id, 'conciseness', conciseness, conciseness_explanation
        FROM experiment_results WHERE conciseness IS NOT NULL
        ON CONFLICT (experiment_result_id, metric) DO NOTHING;

        INSERT INTO experiment_metric_scores (experiment_result_id, metric, score, explanation)
        SELECT id, 'language_consistency', language_consistency, language_consistency_explanation
        FROM experiment_results WHERE language_consistency IS NOT NULL
        ON CONFLICT (experiment_result_id, metric) DO NOTHING;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'ai_usage_record'
    ) THEN
        INSERT INTO ai_usage_events (
            user_id, identity_type, identity_key, model,
            prompt_tokens, completion_tokens, estimated_cost_usd, created_at
        )
        SELECT
            u.id,
            CASE
                WHEN r.user_identifier LIKE 'user:%' THEN 'authenticated'
                WHEN r.user_identifier LIKE 'anon:%' THEN 'anonymous'
                ELSE 'system'
            END,
            r.user_identifier,
            r.model,
            r.prompt_tokens,
            r.completion_tokens,
            r.estimated_cost_usd,
            r.created_at
        FROM ai_usage_record r
        LEFT JOIN users u ON r.user_identifier = 'user:' || u.username
        WHERE NOT EXISTS (
            SELECT 1 FROM ai_usage_events e
            WHERE e.identity_key = r.user_identifier
              AND e.model = r.model
              AND e.created_at = r.created_at
              AND e.prompt_tokens = r.prompt_tokens
        );
    END IF;
END $$;

UPDATE experiment_runs er SET eval_dataset_id = NULL
WHERE er.eval_dataset_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eval_datasets d WHERE d.id = er.eval_dataset_id);

UPDATE dataset_generations dg SET result_dataset_id = NULL
WHERE dg.result_dataset_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eval_datasets d WHERE d.id = dg.result_dataset_id);

ALTER TABLE experiment_runs VALIDATE CONSTRAINT fk_er_eval_dataset;
ALTER TABLE dataset_generations VALIDATE CONSTRAINT fk_dg_result_dataset;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pv_one_active_per_template
    ON prompt_versions (template_id)
    WHERE is_active = true AND template_id IS NOT NULL;
