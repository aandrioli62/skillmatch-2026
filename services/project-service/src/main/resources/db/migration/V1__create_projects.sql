-- V1__create_projects.sql

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_days INT,
    budget NUMERIC(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','OPEN','ASSIGNED','IN_PROGRESS','COMPLETED','CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE project_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL,
    min_reputation_level VARCHAR(20)
);

CREATE TABLE candidatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    professional_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN')),
    cover_letter TEXT,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, professional_id)
);
