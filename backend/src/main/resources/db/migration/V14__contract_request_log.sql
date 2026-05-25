CREATE TABLE IF NOT EXISTS request_log_archive (
    LIKE request_log INCLUDING ALL
);

INSERT INTO request_log_archive
SELECT * FROM request_log
WHERE payload IS NOT NULL AND length(payload) > 0;

ALTER TABLE request_log DROP COLUMN IF EXISTS payload;
