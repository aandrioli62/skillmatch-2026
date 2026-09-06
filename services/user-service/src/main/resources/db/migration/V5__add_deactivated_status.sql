-- V5__add_deactivated_status.sql
-- Admin-initiated permanent account removal, distinct from the reversible SUSPENDED
-- hold: DEACTIVATED also disables the Keycloak identity (see UserServiceImpl.deactivateUser).

ALTER TABLE users DROP CONSTRAINT users_status_check;
ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('PENDING', 'VALIDATED', 'SUSPENDED', 'DEACTIVATED'));
