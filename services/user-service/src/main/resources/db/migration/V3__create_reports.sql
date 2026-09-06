-- A report filed by a professional or company against the counterparty of a
-- collaboration (e.g. a problematic contract). Purely informational until an
-- admin reviews it — closing a report does not itself suspend anyone, that
-- remains the existing separate admin action on the reported user.
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_reported_user_id ON reports (reported_user_id);
