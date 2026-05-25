CREATE TABLE IF NOT EXISTS request_log_archive (
    LIKE request_log INCLUDING ALL
);

-- V1 Flyway baseline never had request_log.payload; only migrate when upgrading legacy DBs.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'request_log'
          AND column_name = 'payload'
    ) THEN
        INSERT INTO request_log_archive
        SELECT * FROM request_log
        WHERE payload IS NOT NULL AND length(payload) > 0;

        ALTER TABLE request_log DROP COLUMN IF EXISTS payload;
    END IF;
END $$;
