CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE remittances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_country VARCHAR(5) NOT NULL,
    corridor VARCHAR(10) NOT NULL,
    partner_ref VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    fee BIGINT NOT NULL,
    exchange_rate NUMERIC(19,6) NOT NULL,
    amount_mmk BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_number VARCHAR(50) NOT NULL,
    received_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_remittances_recipient_user_id ON remittances(recipient_user_id, created_at DESC);
CREATE UNIQUE INDEX idx_remittances_partner_ref ON remittances(partner_ref);
CREATE INDEX idx_remittances_corridor ON remittances(corridor);
CREATE INDEX idx_remittances_status ON remittances(status);
CREATE UNIQUE INDEX idx_remittances_reference_number ON remittances(reference_number);

CREATE TABLE remittance_corridors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(10) NOT NULL,
    source_country VARCHAR(5) NOT NULL,
    source_currency VARCHAR(3) NOT NULL,
    dest_currency VARCHAR(3) NOT NULL DEFAULT 'MMK',
    exchange_rate NUMERIC(19,6) NOT NULL,
    fee_fixed BIGINT NOT NULL,
    fee_percentage NUMERIC(5,4) NOT NULL,
    min_amount BIGINT NOT NULL,
    max_amount BIGINT NOT NULL,
    partner_name VARCHAR(100) NOT NULL,
    partner_api_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_corridors_code ON remittance_corridors(code);
CREATE INDEX idx_corridors_source_country ON remittance_corridors(source_country);
CREATE INDEX idx_corridors_status ON remittance_corridors(status);
