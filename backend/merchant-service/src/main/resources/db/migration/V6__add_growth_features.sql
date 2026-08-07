-- Product tax + digital goods flags
ALTER TABLE products ADD COLUMN tax_rate INT DEFAULT 0;
ALTER TABLE products ADD COLUMN deliverable BOOLEAN DEFAULT false;
ALTER TABLE products ADD COLUMN delivery_content TEXT;

-- Product variants / SKUs
CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    name VARCHAR(150),
    price_delta BIGINT DEFAULT 0,
    quantity BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_pv_product ON product_variants(product_id);

-- Orders with lifecycle + refunds
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    store_id UUID,
    customer_phone VARCHAR(20),
    customer_name VARCHAR(150),
    items JSONB,
    subtotal BIGINT NOT NULL,
    tax BIGINT DEFAULT 0,
    tax_rate INT DEFAULT 0,
    total BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    refund_amount BIGINT DEFAULT 0,
    paid_at TIMESTAMP,
    fulfilled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_orders_merchant ON orders(merchant_id);
CREATE INDEX idx_orders_customer ON orders(customer_phone);
CREATE INDEX idx_orders_status ON orders(status);

-- Digital goods deliveries
CREATE TABLE digital_deliveries (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    order_id UUID,
    product_id UUID,
    content TEXT,
    delivered_to VARCHAR(20),
    delivered_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_dd_order ON digital_deliveries(order_id);

-- Customer notes / activity timeline
CREATE TABLE customer_notes (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    note TEXT NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_cn_merchant ON customer_notes(merchant_id, customer_phone);

-- Payment link auto follow-up
ALTER TABLE payment_links ADD COLUMN auto_follow_up BOOLEAN DEFAULT false;
ALTER TABLE payment_links ADD COLUMN follow_up_hours INT DEFAULT 24;
ALTER TABLE payment_links ADD COLUMN next_reminder_at TIMESTAMP;

-- Notification templates
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    channel VARCHAR(20) DEFAULT 'SMS',
    subject VARCHAR(200),
    body TEXT NOT NULL,
    trigger_event VARCHAR(40),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_nt_merchant ON notification_templates(merchant_id);

-- Withholding / tax invoices
CREATE TABLE tax_invoices (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    invoice_no VARCHAR(40) NOT NULL,
    customer_name VARCHAR(150),
    customer_phone VARCHAR(20),
    subtotal BIGINT NOT NULL,
    tax BIGINT DEFAULT 0,
    withholding_tax BIGINT DEFAULT 0,
    total BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_ti_merchant ON tax_invoices(merchant_id);

-- Chargeback evidence
CREATE TABLE chargeback_evidence (
    id UUID PRIMARY KEY,
    chargeback_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference VARCHAR(255),
    content TEXT,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_cbe_chargeback ON chargeback_evidence(chargeback_id);

-- Webhook retry policy
ALTER TABLE webhook_subscriptions ADD COLUMN max_retries INT DEFAULT 3;
ALTER TABLE webhook_subscriptions ADD COLUMN backoff_minutes INT DEFAULT 5;
ALTER TABLE webhook_deliveries ADD COLUMN retry_count INT DEFAULT 0;
ALTER TABLE webhook_deliveries ADD COLUMN next_retry_at TIMESTAMP;

-- Multi-approver workflows
CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    ref_id UUID,
    initiator_id UUID,
    initiator_name VARCHAR(150),
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by VARCHAR(150),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_ar_merchant ON approval_requests(merchant_id, status);

-- Refunds
CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    order_id UUID,
    transaction_id UUID,
    customer_phone VARCHAR(20),
    amount BIGINT NOT NULL,
    reason VARCHAR(300),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_refund_merchant ON refunds(merchant_id);

-- Store storefront slug
ALTER TABLE stores ADD COLUMN slug VARCHAR(80);
CREATE UNIQUE INDEX idx_store_slug ON stores(slug) WHERE slug IS NOT NULL;

-- Referral registrations / conversions
CREATE TABLE referral_registrations (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    program_id UUID,
    referred_phone VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    bonus_paid BIGINT DEFAULT 0,
    converted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_rr_merchant ON referral_registrations(merchant_id);
