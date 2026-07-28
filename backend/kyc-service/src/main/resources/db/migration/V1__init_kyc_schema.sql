CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE kyc_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    document_type VARCHAR(50),
    status VARCHAR(20),
    reviewed_by UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ka_user_id ON kyc_audit(user_id);
CREATE INDEX idx_ka_action ON kyc_audit(action);
CREATE INDEX idx_ka_status ON kyc_audit(status);
CREATE INDEX idx_ka_reviewed_by ON kyc_audit(reviewed_by);
CREATE INDEX idx_ka_created_at ON kyc_audit(created_at);
