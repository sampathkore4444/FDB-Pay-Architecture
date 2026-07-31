CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(254),
    nrc_number      VARCHAR(30),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    kyc_tier        VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    role            VARCHAR(20)  NOT NULL DEFAULT 'CONSUMER',
    pin_hash        VARCHAR(255),
    pin_attempts    INTEGER      NOT NULL DEFAULT 0,
    pin_locked_until TIMESTAMP,
    referral_code   VARCHAR(10)  UNIQUE,
    referred_by     VARCHAR(10),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_referral_code ON users (referral_code);
CREATE INDEX idx_users_status ON users (status);
