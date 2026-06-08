CREATE TABLE IF NOT EXISTS interview_documents (
    id VARCHAR(64) PRIMARY KEY,
    original_filename VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128),
    parsed_text TEXT NOT NULL,
    char_count INTEGER NOT NULL,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_interview_documents_created_at ON interview_documents (created_at);

CREATE TABLE IF NOT EXISTS interview_sessions (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES interview_documents (id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    voice VARCHAR(32),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_interview_sessions_status ON interview_sessions (status);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_started_at ON interview_sessions (started_at);

CREATE TABLE IF NOT EXISTS interview_transcript_turns (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    text TEXT NOT NULL,
    sequence_no INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_interview_turns_session_seq ON interview_transcript_turns (session_id, sequence_no);

CREATE TABLE IF NOT EXISTS interview_transcripts (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE REFERENCES interview_sessions (id) ON DELETE CASCADE,
    raw_text TEXT,
    cleaned_text TEXT,
    clean_status VARCHAR(32) NOT NULL,
    ingested_document_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    cleaned_at TIMESTAMPTZ
);
