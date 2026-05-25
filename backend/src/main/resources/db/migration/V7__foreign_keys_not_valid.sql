ALTER TABLE experiment_runs
    ADD CONSTRAINT fk_er_eval_dataset
    FOREIGN KEY (eval_dataset_id) REFERENCES eval_datasets (id) NOT VALID;

ALTER TABLE dataset_generations
    ADD CONSTRAINT fk_dg_result_dataset
    FOREIGN KEY (result_dataset_id) REFERENCES eval_datasets (id) NOT VALID;
