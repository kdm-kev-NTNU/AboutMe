CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    language VARCHAR(8),
    provider VARCHAR(32),
    CONSTRAINT uq_prompt_templates_variant UNIQUE (name, language, provider)
);

ALTER TABLE prompt_versions
    ADD COLUMN IF NOT EXISTS template_id BIGINT REFERENCES prompt_templates (id);

CREATE INDEX IF NOT EXISTS idx_pv_template ON prompt_versions (template_id);
