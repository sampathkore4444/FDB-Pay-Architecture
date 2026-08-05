ALTER TABLE transactions ADD COLUMN IF NOT EXISTS parent_transaction_id UUID;

CREATE INDEX IF NOT EXISTS idx_tx_parent ON transactions(parent_transaction_id);
