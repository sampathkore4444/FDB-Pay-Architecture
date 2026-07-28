CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'MMK',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    balance_total BIGINT NOT NULL DEFAULT 0,
    balance_held BIGINT NOT NULL DEFAULT 0,
    balance_frozen BIGINT NOT NULL DEFAULT 0,
    daily_limit BIGINT NOT NULL DEFAULT 500000,
    monthly_limit BIGINT NOT NULL DEFAULT 5000000,
    kyc_tier VARCHAR(20) NOT NULL DEFAULT 'NONE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE UNIQUE INDEX idx_wallets_user_id_active ON wallets(user_id) WHERE status = 'ACTIVE';

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    type VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    txn_id UUID NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_wallet_id ON ledger_entries(wallet_id, created_at DESC);
CREATE INDEX idx_ledger_entries_txn_id ON ledger_entries(txn_id);
