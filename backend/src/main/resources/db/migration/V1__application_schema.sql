-- Application tables (vector_store is managed by Spring AI pgvector auto-config).

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_usage_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    identity_type VARCHAR(16) NOT NULL,
    identity_key VARCHAR(256) NOT NULL,
    model VARCHAR(128) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    estimated_cost_usd NUMERIC(19, 8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_identity_created ON ai_usage_events (identity_type, identity_key, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_user_created ON ai_usage_events (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_created ON ai_usage_events (created_at);

CREATE TABLE IF NOT EXISTS prompt_versions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    version INT NOT NULL,
    language VARCHAR(8),
    provider VARCHAR(32),
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL,
    description TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pv_name ON prompt_versions (name);

CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    language VARCHAR(8),
    provider VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) PRIMARY KEY,
    filename VARCHAR(512) NOT NULL,
    source_uri TEXT,
    content_hash VARCHAR(64) NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    replaced_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS feedback_submission (
    id BIGSERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS request_log (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(255) NOT NULL,
    method VARCHAR(255) NOT NULL,
    user_id BIGINT,
    status_code SMALLINT,
    duration_ms INT,
    requester_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS eval_datasets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_eval_ds_created ON eval_datasets (created_at);

CREATE TABLE IF NOT EXISTS eval_dataset_examples (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES eval_datasets (id),
    question TEXT NOT NULL,
    reference_text TEXT
);

CREATE INDEX IF NOT EXISTS idx_eval_ex_ds ON eval_dataset_examples (dataset_id);

CREATE TABLE IF NOT EXISTS experiment_runs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    eval_dataset_id BIGINT REFERENCES eval_datasets (id),
    generator_model VARCHAR(128) NOT NULL,
    evaluator_model VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_examples INT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_er_created ON experiment_runs (created_at);

CREATE TABLE IF NOT EXISTS experiment_results (
    id BIGSERIAL PRIMARY KEY,
    experiment_run_id BIGINT NOT NULL REFERENCES experiment_runs (id),
    eval_example_id BIGINT REFERENCES eval_dataset_examples (id),
    question TEXT NOT NULL,
    reference_answer TEXT,
    rag_response TEXT NOT NULL,
    retrieved_context TEXT
);

CREATE INDEX IF NOT EXISTS idx_expres_run ON experiment_results (experiment_run_id);
CREATE INDEX IF NOT EXISTS idx_expres_eval_example ON experiment_results (eval_example_id);

CREATE TABLE IF NOT EXISTS experiment_metric_scores (
    id BIGSERIAL PRIMARY KEY,
    experiment_result_id BIGINT NOT NULL REFERENCES experiment_results (id),
    metric VARCHAR(64) NOT NULL,
    score DOUBLE PRECISION,
    explanation TEXT
);

CREATE TABLE IF NOT EXISTS dataset_generations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    description TEXT,
    document_id_filter VARCHAR(128),
    model VARCHAR(128) NOT NULL,
    questions_per_chunk INT NOT NULL,
    max_questions INT,
    seed INT,
    status VARCHAR(32) NOT NULL,
    questions_generated INT,
    result_dataset_id BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_dataset_gen_created ON dataset_generations (created_at);
