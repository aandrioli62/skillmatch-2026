-- V1__create_transactions.sql

CREATE TABLE commission_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rate_percentage NUMERIC(5,2) NOT NULL DEFAULT 8.00,
    effective_from TIMESTAMP NOT NULL DEFAULT NOW(),
    set_by_admin_id UUID
);

-- Platform default commission rate (CLAUDE.md business rule #2), effective from day one.
INSERT INTO commission_config (rate_percentage) VALUES (8.00);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    company_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    commission_amount NUMERIC(10,2) NOT NULL,
    net_amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED' CHECK (status IN ('INITIATED','PROCESSING','COMPLETED','FAILED','REFUNDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    UNIQUE(contract_id)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL UNIQUE REFERENCES transactions(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    company_id UUID NOT NULL,
    total NUMERIC(10,2) NOT NULL,
    commission NUMERIC(10,2) NOT NULL,
    professional_fee NUMERIC(10,2) NOT NULL,
    pdf_url VARCHAR(500),
    issued_at TIMESTAMP NOT NULL DEFAULT NOW()
);
