-- V1__create_contracts.sql

CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    company_id UUID NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    commission_rate NUMERIC(5,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PENDING_SIGNATURES','ACTIVE','COMPLETED','CANCELLED')),
    terms TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    signed_at TIMESTAMP,
    UNIQUE(project_id)
);
