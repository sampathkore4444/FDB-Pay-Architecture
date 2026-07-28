CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    type VARCHAR(30) NOT NULL,
    funding_type VARCHAR(20) NOT NULL,
    merchant_id UUID,
    discount_value BIGINT NOT NULL,
    max_discount BIGINT,
    min_transaction_amount BIGINT,
    max_usage_total INT NOT NULL DEFAULT 0,
    max_usage_per_user INT NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    promo_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_promotions_status ON promotions(status);
CREATE INDEX idx_promotions_merchant_id ON promotions(merchant_id);
CREATE UNIQUE INDEX idx_promotions_promo_code ON promotions(promo_code) WHERE promo_code IS NOT NULL;
CREATE INDEX idx_promotions_active_dates ON promotions(status, start_date, end_date);

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id),
    user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    discount_applied BIGINT NOT NULL DEFAULT 0,
    cashback_amount BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promotion_usages_promotion ON promotion_usages(promotion_id);
CREATE INDEX idx_promotion_usages_user ON promotion_usages(user_id, promotion_id);
CREATE INDEX idx_promotion_usages_transaction ON promotion_usages(transaction_id);

CREATE TABLE cashback_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    total_earned BIGINT NOT NULL DEFAULT 0,
    total_redeemed BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_cashback_wallets_user ON cashback_wallets(user_id);

CREATE TABLE cashback_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cashback_wallet_id UUID NOT NULL REFERENCES cashback_wallets(id),
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    promotion_id UUID,
    transaction_id UUID,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cashback_txn_wallet ON cashback_transactions(cashback_wallet_id, created_at DESC);
CREATE INDEX idx_cashback_txn_promotion ON cashback_transactions(promotion_id);
