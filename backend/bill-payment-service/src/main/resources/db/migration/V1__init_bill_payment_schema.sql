CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE bill_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    biller_id UUID NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    transaction_ref VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bp_user_id ON bill_payments(user_id);
CREATE INDEX idx_bp_biller_id ON bill_payments(biller_id);
CREATE INDEX idx_bp_account_number ON bill_payments(account_number);
CREATE INDEX idx_bp_status ON bill_payments(status);

CREATE TABLE airtime_topups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    phone VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_at_user_id ON airtime_topups(user_id);
CREATE INDEX idx_at_provider ON airtime_topups(provider);
CREATE INDEX idx_at_status ON airtime_topups(status);
CREATE INDEX idx_at_created_at ON airtime_topups(created_at);
