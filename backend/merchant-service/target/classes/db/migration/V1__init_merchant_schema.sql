CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50),
    business_license VARCHAR(100),
    tax_id VARCHAR(50),
    settlement_account VARCHAR(100),
    settlement_type VARCHAR(5),
    fee_schedule VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    category VARCHAR(50),
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    qr_static_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_user_id ON merchants(user_id);
CREATE INDEX idx_merchant_license ON merchants(business_license);
CREATE INDEX idx_merchant_tax_id ON merchants(tax_id);
CREATE INDEX idx_merchant_status ON merchants(status);

CREATE TABLE staff_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sa_merchant_id ON staff_accounts(merchant_id);
CREATE INDEX idx_sa_user_id ON staff_accounts(user_id);

CREATE TABLE pos_terminals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_ping_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pt_merchant_id ON pos_terminals(merchant_id);
CREATE INDEX idx_pt_serial ON pos_terminals(serial_number);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    customer_phone VARCHAR(20),
    customer_name VARCHAR(200),
    items JSONB,
    subtotal BIGINT NOT NULL,
    tax BIGINT NOT NULL,
    total BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    due_date DATE,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_merchant_id ON invoices(merchant_id);
CREATE INDEX idx_inv_status ON invoices(status);
CREATE INDEX idx_inv_customer_phone ON invoices(customer_phone);
