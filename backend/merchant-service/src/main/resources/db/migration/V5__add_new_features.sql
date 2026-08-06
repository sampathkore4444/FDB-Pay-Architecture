-- Product stock tracking
ALTER TABLE products ADD COLUMN quantity BIGINT DEFAULT 0;
ALTER TABLE products ADD COLUMN low_stock_threshold BIGINT DEFAULT 0;

-- API key environment + usage analytics
ALTER TABLE api_keys ADD COLUMN environment VARCHAR(20) DEFAULT 'LIVE';
ALTER TABLE api_keys ADD COLUMN usage_count BIGINT DEFAULT 0;

-- Payment link reminder tracking
ALTER TABLE payment_links ADD COLUMN reminder_count INT DEFAULT 0;

-- On-demand payouts
CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    account_id UUID NOT NULL,
    account_label VARCHAR(150) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    reference VARCHAR(100),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT now(),
    completed_at TIMESTAMP
);
CREATE INDEX idx_payout_merchant ON payouts(merchant_id);

-- Webhook subscriptions
CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    event VARCHAR(40) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_whsub_merchant ON webhook_subscriptions(merchant_id);

-- Webhook delivery log
CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    subscription_id UUID,
    event VARCHAR(40) NOT NULL,
    url VARCHAR(500) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL,
    attempts INT DEFAULT 1,
    status_code INT,
    error VARCHAR(500),
    created_at TIMESTAMP DEFAULT now(),
    delivered_at TIMESTAMP
);
CREATE INDEX idx_whdel_merchant ON webhook_deliveries(merchant_id);

-- Custom fraud rules
CREATE TABLE fraud_rules (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    threshold BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_fraud_merchant ON fraud_rules(merchant_id);

-- Automated marketing campaigns
CREATE TABLE marketing_campaigns (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    campaign_type VARCHAR(30) NOT NULL,
    audience_segment VARCHAR(50) NOT NULL,
    discount_code_id UUID,
    cashback_id UUID,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_campaign_merchant ON marketing_campaigns(merchant_id);
