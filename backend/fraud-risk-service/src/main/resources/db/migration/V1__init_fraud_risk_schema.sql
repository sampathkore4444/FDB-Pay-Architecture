CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    user_id UUID NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_fa_transaction_id ON fraud_alerts(transaction_id);
CREATE INDEX idx_fa_user_id ON fraud_alerts(user_id);
CREATE INDEX idx_fa_alert_type ON fraud_alerts(alert_type);
CREATE INDEX idx_fa_severity ON fraud_alerts(severity);
CREATE INDEX idx_fa_status ON fraud_alerts(status);
CREATE INDEX idx_fa_created_at ON fraud_alerts(created_at);
