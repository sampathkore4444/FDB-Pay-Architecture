CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    complainant_user_id UUID NOT NULL,
    respondent_user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    amount BIGINT NOT NULL DEFAULT 0,
    description TEXT,
    resolution TEXT,
    resolved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_dispute_transaction ON disputes(transaction_id);
CREATE INDEX idx_dispute_complainant ON disputes(complainant_user_id);
CREATE INDEX idx_dispute_respondent ON disputes(respondent_user_id);
CREATE INDEX idx_dispute_status ON disputes(status);
CREATE INDEX idx_dispute_created_at ON disputes(created_at);

CREATE TABLE dispute_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_dispute ON dispute_evidence(dispute_id);
CREATE INDEX idx_evidence_uploaded_by ON dispute_evidence(uploaded_by);
