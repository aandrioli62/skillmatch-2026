-- SkillMatch — initialise all logical databases on a single PostgreSQL instance
-- Run once on first deploy: psql -U postgres -f init-databases.sql

CREATE USER skillmatch WITH PASSWORD 'secret';

CREATE DATABASE "user-db"         OWNER skillmatch;
CREATE DATABASE "project-db"      OWNER skillmatch;
CREATE DATABASE "contract-db"     OWNER skillmatch;
CREATE DATABASE "payment-db"      OWNER skillmatch;
CREATE DATABASE "feedback-db"     OWNER skillmatch;
CREATE DATABASE "identity-db"     OWNER skillmatch;  -- Keycloak

GRANT ALL PRIVILEGES ON DATABASE "user-db"      TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE "project-db"   TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE "contract-db"  TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE "payment-db"   TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE "feedback-db"  TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE "identity-db"  TO skillmatch;
