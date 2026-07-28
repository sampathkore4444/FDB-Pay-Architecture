CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE report_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type VARCHAR(100) NOT NULL,
    parameters JSONB NOT NULL,
    result JSONB NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rc_report_type ON report_cache(report_type);
CREATE INDEX idx_rc_expires_at ON report_cache(expires_at);
CREATE INDEX idx_rc_generated_at ON report_cache(generated_at);
