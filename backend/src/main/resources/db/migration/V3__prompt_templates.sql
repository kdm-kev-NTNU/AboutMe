CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    language VARCHAR(8),
    provider VARCHAR(32)
);

-- V1 may have created prompt_templates without this constraint; ensure it exists for V10 ON CONFLICT.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_prompt_templates_variant'
          AND conrelid = 'public.prompt_templates'::regclass
    ) THEN
        ALTER TABLE prompt_templates
            ADD CONSTRAINT uq_prompt_templates_variant UNIQUE (name, language, provider);
    END IF;
END $$;

ALTER TABLE prompt_versions
    ADD COLUMN IF NOT EXISTS template_id BIGINT REFERENCES prompt_templates (id);

CREATE INDEX IF NOT EXISTS idx_pv_template ON prompt_versions (template_id);
