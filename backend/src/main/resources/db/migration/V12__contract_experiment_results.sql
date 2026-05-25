ALTER TABLE experiment_results RENAME COLUMN documents TO retrieved_context;

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
