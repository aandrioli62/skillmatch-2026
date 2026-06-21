-- SkillMatch — initialise all logical databases on a single PostgreSQL instance
-- identitydb is created automatically via POSTGRES_DB env var
-- skillmatch user is created automatically via POSTGRES_USER env var

CREATE DATABASE userdb;
CREATE DATABASE projectdb;
CREATE DATABASE contractdb;
CREATE DATABASE paymentdb;
CREATE DATABASE feedbackdb;

GRANT ALL PRIVILEGES ON DATABASE userdb      TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE projectdb   TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE contractdb  TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE paymentdb   TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE feedbackdb  TO skillmatch;
