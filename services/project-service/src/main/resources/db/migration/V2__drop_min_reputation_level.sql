-- V2__drop_min_reputation_level.sql
-- The minimum-reputation-level requirement was collected at project creation but never
-- enforced at candidature time nor shown to professionals — removed rather than wired
-- up, to avoid a field that implies a guarantee the platform doesn't actually make.

ALTER TABLE project_requirements DROP COLUMN min_reputation_level;
