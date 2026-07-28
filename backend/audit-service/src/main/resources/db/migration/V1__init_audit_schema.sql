-- Audit entries table
-- Note: For production, consider partitioning by month using native PostgreSQL partitioning:
-- CREATE TABLE audit_entries (...) PARTITION BY RANGE (created_at);
-- Then create monthly partitions: audit_entries_2026_01, audit_entries_2026_02, etc.

CREATE TABLE audit_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_name VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor ON audit_entries(actor_id);
CREATE INDEX idx_audit_resource ON audit_entries(resource_type, resource_id);
CREATE INDEX idx_audit_action ON audit_entries(action);
CREATE INDEX idx_audit_created_at ON audit_entries(created_at);
CREATE INDEX idx_audit_actor_type ON audit_entries(actor_type);

-- Partial indexes for common queries
CREATE INDEX idx_audit_created_at_recent ON audit_entries(created_at)
    WHERE created_at > NOW() - INTERVAL '30 days';
