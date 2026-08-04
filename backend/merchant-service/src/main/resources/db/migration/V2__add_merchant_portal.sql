ALTER TABLE merchants ADD COLUMN IF NOT EXISTS rolling_reserve_percent INT NOT NULL DEFAULT 0;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS rolling_reserve_period_days INT NOT NULL DEFAULT 7;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS rolling_reserve_balance BIGINT NOT NULL DEFAULT 0;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS terminal_fields JSONB;

CREATE TABLE IF NOT EXISTS payment_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    description TEXT,
    customer_phone VARCHAR(20),
    customer_name VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    single_use BOOLEAN NOT NULL DEFAULT TRUE,
    paid_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_link_merchant ON payment_links(merchant_id);
CREATE INDEX idx_payment_link_token ON payment_links(token);
CREATE INDEX idx_payment_link_status ON payment_links(status);
