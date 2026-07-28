CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE agent_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    wallet_id UUID NOT NULL,
    float_balance BIGINT NOT NULL DEFAULT 0,
    commission_balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit BIGINT NOT NULL DEFAULT 50000000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_user_id ON agent_accounts(user_id);

CREATE TABLE agent_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_user_id UUID NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_atx_agent_user_id ON agent_transactions(agent_user_id);
CREATE INDEX idx_atx_customer_phone ON agent_transactions(customer_phone);
CREATE INDEX idx_atx_created_at ON agent_transactions(created_at);

CREATE TABLE commission_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    commission_rate NUMERIC(10,6) NOT NULL,
    commission_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    earned_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cr_agent_user_id ON commission_records(agent_user_id);
CREATE INDEX idx_cr_transaction_id ON commission_records(transaction_id);
CREATE INDEX idx_cr_status ON commission_records(status);
CREATE INDEX idx_cr_earned_at ON commission_records(earned_at);
