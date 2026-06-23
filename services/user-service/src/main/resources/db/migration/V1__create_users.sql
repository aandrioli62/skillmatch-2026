-- V1__create_users.sql

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PROFESSIONAL', 'COMPANY', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VALIDATED', 'SUSPENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE professional_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    payment_account VARCHAR(255),
    reputation_level VARCHAR(20) DEFAULT 'JUNIOR' CHECK (reputation_level IN ('JUNIOR', 'AFFIDABILE', 'TOP_PERFORMER')),
    avg_rating NUMERIC(3,2) DEFAULT 0,
    total_reviews INT DEFAULT 0
);

CREATE TABLE company_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    company_name VARCHAR(255) NOT NULL,
    vat_number VARCHAR(50),
    address TEXT,
    contact_person VARCHAR(200)
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(100)
);

CREATE TABLE user_skills (
    user_id UUID NOT NULL REFERENCES users(id),
    skill_id UUID NOT NULL REFERENCES skills(id),
    certification_url VARCHAR(500),
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
