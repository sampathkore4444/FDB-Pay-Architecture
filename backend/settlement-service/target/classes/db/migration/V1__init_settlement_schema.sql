CREATE TABLE settlement_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_date DATE NOT NULL UNIQUE,
    total_merchants INT NOT NULL DEFAULT 0,
    total_gross_amount BIGINT NOT NULL DEFAULT 0,
    total_fees BIGINT NOT NULL DEFAULT 0,
    total_net_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_batch_status ON settlement_batches(status);
CREATE INDEX idx_batch_date ON settlement_batches(batch_date);

CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    batch_id UUID NOT NULL REFERENCES settlement_batches(id),
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    gross_amount BIGINT NOT NULL DEFAULT 0,
    fees BIGINT NOT NULL DEFAULT 0,
    net_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMPTZ,
    settlement_ref VARCHAR(50),
    transaction_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_settlement_merchant ON settlements(merchant_id);
CREATE INDEX idx_settlement_status ON settlements(status);
CREATE INDEX idx_settlement_batch ON settlements(batch_id);
CREATE INDEX idx_settlement_period ON settlements(period_start, period_end);
CREATE INDEX idx_settlement_created_at ON settlements(created_at);
