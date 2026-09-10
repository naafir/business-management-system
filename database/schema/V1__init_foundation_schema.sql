-- ==============================================================================
-- Flyway Foundation Schema Migration (V1)
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. Business Settings table
CREATE TABLE IF NOT EXISTS business_settings (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(200) NOT NULL,
    trade_name VARCHAR(200),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_name VARCHAR(100) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    pin_code VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(150),
    logo_url VARCHAR(500),
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    bank_branch VARCHAR(100),
    bank_upi_id VARCHAR(100),
    invoice_prefix VARCHAR(20) NOT NULL DEFAULT 'INV-',
    invoice_next_seq BIGINT NOT NULL DEFAULT 1,
    invoice_terms TEXT,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. GST Rates table
CREATE TABLE IF NOT EXISTS gst_rates (
    id UUID PRIMARY KEY,
    rate_percent NUMERIC(5,2) NOT NULL UNIQUE,
    description VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    ip_address VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- 5. Backup Records table
CREATE TABLE IF NOT EXISTS backup_records (
    id UUID PRIMARY KEY,
    backup_type VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- Seeds
-- ------------------------------------------------------------------------------

INSERT INTO gst_rates (id, rate_percent, description, active)
VALUES 
    ('11111111-1111-1111-1111-111111110000', 0.00, 'Exempt / Nil Rated (0%)', TRUE),
    ('11111111-1111-1111-1111-111111110005', 5.00, 'Standard Lower Rate (5%)', TRUE),
    ('11111111-1111-1111-1111-111111110012', 12.00, 'Standard Medium Rate (12%)', TRUE),
    ('11111111-1111-1111-1111-111111110018', 18.00, 'Standard High Rate (18%)', TRUE),
    ('11111111-1111-1111-1111-111111110028', 28.00, 'Peak Rate (28%)', TRUE);

-- Seed default initial admin (Password: Admin@SecurePass2026!)
INSERT INTO users (id, username, email, password_hash, full_name, role, active)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'admin',
    'owner@mybusiness.local',
    '$2a$12$NqBqTqF56fWw8Yn7pZ.5r.nNq6Hn/zY0.41x83a00GjS0x1O2Nn0W',
    'Business Owner',
    'OWNER',
    TRUE
);

-- Seed default business settings
INSERT INTO business_settings (
    id, legal_name, trade_name, state_name, state_code, invoice_prefix, invoice_next_seq, default_currency
) VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'My Enterprise Pvt Ltd',
    'My Enterprise',
    'Maharashtra',
    '27',
    'INV-2026-',
    1,
    'INR'
);