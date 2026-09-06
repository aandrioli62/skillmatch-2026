-- V4__add_company_profile_fields.sql

ALTER TABLE company_profiles ADD COLUMN description TEXT;
ALTER TABLE company_profiles ADD COLUMN payment_account VARCHAR(255);
