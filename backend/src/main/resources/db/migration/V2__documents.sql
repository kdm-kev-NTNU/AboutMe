CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) PRIMARY KEY,
    filename VARCHAR(512) NOT NULL,
    source_uri TEXT,
    content_hash VARCHAR(64) NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    replaced_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_documents_ingested ON documents (ingested_at);
CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents (content_hash);
