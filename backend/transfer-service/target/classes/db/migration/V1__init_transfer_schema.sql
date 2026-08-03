CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sender_wallet_id UUID NOT NULL,
    receiver_wallet_id UUID NOT NULL,
    amount BIGINT NOT NULL,
    fee BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failure_reason TEXT
);

CREATE INDEX idx_tx_sender ON transactions(sender_wallet_id);
CREATE INDEX idx_tx_receiver ON transactions(receiver_wallet_id);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_created_at ON transactions(created_at);

CREATE TABLE money_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_user_id UUID NOT NULL,
    target_phone VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_id UUID,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mr_requester ON money_requests(requester_user_id);
CREATE INDEX idx_mr_target_phone ON money_requests(target_phone);
CREATE INDEX idx_mr_status ON money_requests(status);

CREATE TABLE scheduled_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    recipient_identifier VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    next_execution_date DATE,
    last_execution_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    total_executions INT NOT NULL DEFAULT 12,
    completed_executions INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sp_user_id ON scheduled_payments(user_id);
CREATE INDEX idx_sp_status ON scheduled_payments(status);
CREATE INDEX idx_sp_next_execution ON scheduled_payments(next_execution_date);
