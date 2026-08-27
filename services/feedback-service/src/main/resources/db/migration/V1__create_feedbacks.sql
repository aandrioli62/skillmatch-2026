-- V1__create_feedbacks.sql

-- Not part of the original CLAUDE-SERVICES.md schema: caches the two parties of a
-- project once payment.completed is received, so a feedback submission can be
-- authorized and routed to the correct reviewee without a synchronous call back
-- to Project/Contract Service.
CREATE TABLE feedback_eligibility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL UNIQUE,
    company_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    enabled_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewee_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, reviewer_id, reviewee_id)
);
