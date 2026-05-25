ALTER TABLE request_log
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users (id);

ALTER TABLE request_log
    ADD COLUMN IF NOT EXISTS status_code SMALLINT;

ALTER TABLE request_log
    ADD COLUMN IF NOT EXISTS duration_ms INT;

CREATE INDEX IF NOT EXISTS idx_request_log_user_created ON request_log (user_id, created_at);
