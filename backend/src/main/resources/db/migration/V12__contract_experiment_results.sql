DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'experiment_results'
          AND column_name = 'documents'
    ) THEN
        ALTER TABLE experiment_results RENAME COLUMN documents TO retrieved_context;
    END IF;
END $$;

ALTER TABLE experiment_results DROP COLUMN IF EXISTS faithfulness;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS relevance;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS correctness;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS conciseness;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS faithfulness_explanation;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS relevance_explanation;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS correctness_explanation;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS conciseness_explanation;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS language_consistency;
ALTER TABLE experiment_results DROP COLUMN IF EXISTS language_consistency_explanation;
