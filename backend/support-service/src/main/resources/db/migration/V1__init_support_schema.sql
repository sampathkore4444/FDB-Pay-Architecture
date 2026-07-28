CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE account_managers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    max_clients INT NOT NULL DEFAULT 50,
    current_clients INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_managers_user_id ON account_managers(user_id);
CREATE INDEX idx_account_managers_status ON account_managers(status);

CREATE TABLE support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_user_id UUID NOT NULL,
    subject VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_manager_id UUID REFERENCES account_managers(id),
    last_response_at TIMESTAMPTZ,
    sla_deadline TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_tickets_corporate_user ON support_tickets(corporate_user_id, created_at DESC);
CREATE INDEX idx_tickets_manager ON support_tickets(assigned_manager_id, created_at DESC);
CREATE INDEX idx_tickets_status ON support_tickets(status);
CREATE INDEX idx_tickets_sla ON support_tickets(sla_deadline) WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'WAITING_INTERNAL');

CREATE TABLE ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    attachments JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_messages_ticket ON ticket_messages(ticket_id, created_at ASC);
CREATE INDEX idx_ticket_messages_sender ON ticket_messages(sender_id);
