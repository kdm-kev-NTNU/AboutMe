CREATE TABLE IF NOT EXISTS ai_usage_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users (id),
    identity_type VARCHAR(16) NOT NULL,
    identity_key VARCHAR(256) NOT NULL,
    model VARCHAR(128) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    estimated_cost_usd NUMERIC(19, 8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_identity_created
    ON ai_usage_events (identity_type, identity_key, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_user_created ON ai_usage_events (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_ev_created ON ai_usage_events (created_at);
