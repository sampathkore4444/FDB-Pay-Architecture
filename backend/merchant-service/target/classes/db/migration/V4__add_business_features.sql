ALTER TABLE merchants
    ADD COLUMN alert_large_order_threshold BIGINT DEFAULT 500000,
    ADD COLUMN alert_daily_surge_threshold BIGINT DEFAULT 100000,
    ADD COLUMN settlement_preferred_time VARCHAR(5) DEFAULT '16:00',
    ADD COLUMN webhook_url VARCHAR(255);

CREATE TABLE merchant_audit_log (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    actor_type VARCHAR(20),
    actor_name VARCHAR(150),
    staff_id UUID,
    action VARCHAR(50) NOT NULL,
    entity VARCHAR(50),
    entity_id VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_audit_merchant ON merchant_audit_log(merchant_id, created_at DESC);

CREATE TABLE recurring_plans (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    amount BIGINT NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_name VARCHAR(150),
    interval VARCHAR(20) DEFAULT 'MONTHLY',
    day_of_week INT,
    day_of_month INT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    max_charges INT,
    charge_count INT DEFAULT 0,
    next_run_at TIMESTAMP,
    last_charge_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_rp_merchant ON recurring_plans(merchant_id);

CREATE TABLE payout_accounts (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch VARCHAR(100),
    is_default BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_pa_merchant ON payout_accounts(merchant_id);

CREATE TABLE discount_codes (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    type VARCHAR(20) NOT NULL,
    value BIGINT NOT NULL,
    min_spend BIGINT DEFAULT 0,
    max_uses INT,
    used_count INT DEFAULT 0,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_dc_merchant ON discount_codes(merchant_id);

CREATE TABLE cashback_campaigns (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    percent INT NOT NULL,
    budget BIGINT NOT NULL,
    spent BIGINT DEFAULT 0,
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_cc_merchant ON cashback_campaigns(merchant_id);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    price BIGINT NOT NULL,
    description VARCHAR(255),
    category VARCHAR(100),
    image_url VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_prod_merchant ON products(merchant_id);

CREATE TABLE merchant_reviews (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    customer_name VARCHAR(150),
    customer_phone VARCHAR(20),
    rating INT NOT NULL,
    comment TEXT,
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    admin_reply TEXT,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_review_merchant ON merchant_reviews(merchant_id);

CREATE TABLE loyalty_settings (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    points_per_mmk INT DEFAULT 1,
    reward_threshold_points INT DEFAULT 1000,
    reward_value BIGINT DEFAULT 1000,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_loyalty_merchant ON loyalty_settings(merchant_id);

CREATE TABLE referral_programs (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    referral_bonus BIGINT DEFAULT 0,
    referred_bonus BIGINT DEFAULT 0,
    uses INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_ref_merchant ON referral_programs(merchant_id);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    key_preview VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_apikey_merchant ON api_keys(merchant_id);

CREATE TABLE report_templates (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    report_type VARCHAR(30) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    format VARCHAR(10) DEFAULT 'CSV',
    email VARCHAR(150),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_rt_merchant ON report_templates(merchant_id);
