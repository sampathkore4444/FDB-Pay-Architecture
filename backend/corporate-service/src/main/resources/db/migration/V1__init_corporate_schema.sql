CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE bulk_disbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_user_id UUID NOT NULL,
    file_ref VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_rows INT NOT NULL DEFAULT 0,
    successful_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_bd_corporate_user_id ON bulk_disbursements(corporate_user_id);
CREATE INDEX idx_bd_status ON bulk_disbursements(status);
CREATE INDEX idx_bd_created_at ON bulk_disbursements(created_at);

CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_user_id UUID NOT NULL,
    period VARCHAR(10) NOT NULL,
    total_employees INT NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_by UUID,
    approved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_pr_corporate_user_id ON payroll_runs(corporate_user_id);
CREATE INDEX idx_pr_status ON payroll_runs(status);
CREATE INDEX idx_pr_period ON payroll_runs(period);

CREATE TABLE payroll_employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_run_id UUID NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_ref VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pe_payroll_run_id ON payroll_employees(payroll_run_id);
CREATE INDEX idx_pe_employee_id ON payroll_employees(employee_id);
CREATE INDEX idx_pe_status ON payroll_employees(status);

CREATE TABLE payroll_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_user_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ps_corporate_user_id ON payroll_schedules(corporate_user_id);
CREATE INDEX idx_ps_scheduled_date ON payroll_schedules(scheduled_date);
CREATE INDEX idx_ps_status ON payroll_schedules(status);

CREATE TABLE approval_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bulk_disbursement_id UUID NOT NULL,
    approver_user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    comments VARCHAR(500),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_aw_bulk_disbursement_id ON approval_workflows(bulk_disbursement_id);
CREATE INDEX idx_aw_approver_user_id ON approval_workflows(approver_user_id);
CREATE INDEX idx_aw_status ON approval_workflows(status);
