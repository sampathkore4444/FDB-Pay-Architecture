ALTER TABLE staff_accounts
    ADD COLUMN store_id UUID,
    ADD COLUMN permissions TEXT;

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_store_merchant ON stores(merchant_id);

CREATE TABLE chargebacks (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    transaction_id UUID,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'MMK',
    reason_code VARCHAR(50),
    status VARCHAR(20) DEFAULT 'OPEN',
    customer_notes TEXT,
    deadline TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_cb_merchant ON chargebacks(merchant_id);

CREATE TABLE chargeback_notes (
    id UUID PRIMARY KEY,
    chargeback_id UUID NOT NULL,
    author_type VARCHAR(20),
    author_name VARCHAR(150),
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_cbn_chargeback ON chargeback_notes(chargeback_id);

CREATE TABLE financing_applications (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    requested_amount BIGINT NOT NULL,
    term_months INT NOT NULL,
    purpose VARCHAR(100),
    monthly_revenue BIGINT DEFAULT 0,
    estimated_limit BIGINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    admin_note TEXT,
    created_at TIMESTAMP DEFAULT now(),
    reviewed_at TIMESTAMP
);
CREATE INDEX idx_fa_merchant ON financing_applications(merchant_id);

CREATE TABLE risk_alerts (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    alert_type VARCHAR(50),
    severity VARCHAR(10) DEFAULT 'MEDIUM',
    title VARCHAR(200),
    message TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT now(),
    acknowledged_at TIMESTAMP
);
CREATE INDEX idx_ra_merchant ON risk_alerts(merchant_id);
