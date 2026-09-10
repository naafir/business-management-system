-- ==============================================================================
-- Flyway Migration V2: Products, Categories, Customers & Suppliers
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Product Categories table
CREATE TABLE IF NOT EXISTS product_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_categories_name ON product_categories(name);

-- 2. Products table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES product_categories(id),
    brand VARCHAR(100),
    hsn_sac VARCHAR(20) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    purchase_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    selling_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    gst_rate_id UUID REFERENCES gst_rates(id),
    opening_stock NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    current_stock NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    min_stock_level NUMERIC(15,2) NOT NULL DEFAULT 5.00,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);

-- 3. Customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    business_name VARCHAR(200),
    phone VARCHAR(20),
    email VARCHAR(100),
    billing_address TEXT,
    shipping_address TEXT,
    city VARCHAR(100),
    state_name VARCHAR(100) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    pin_code VARCHAR(10),
    gstin VARCHAR(15),
    customer_type VARCHAR(20) NOT NULL DEFAULT 'B2C',
    credit_limit NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    outstanding_balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_gstin ON customers(gstin);
CREATE INDEX IF NOT EXISTS idx_customers_active ON customers(active);

-- 4. Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    business_name VARCHAR(200),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    city VARCHAR(100),
    state_name VARCHAR(100) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    pin_code VARCHAR(10),
    gstin VARCHAR(15),
    outstanding_balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(name);
CREATE INDEX IF NOT EXISTS idx_suppliers_phone ON suppliers(phone);
CREATE INDEX IF NOT EXISTS idx_suppliers_gstin ON suppliers(gstin);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON suppliers(active);

-- ------------------------------------------------------------------------------
-- Seeds: Sample Product Categories
-- ------------------------------------------------------------------------------
INSERT INTO product_categories (id, name, description, active)
VALUES 
    ('22222222-2222-2222-2222-222222220001', 'General Goods', 'Standard merchandise and general products', TRUE),
    ('22222222-2222-2222-2222-222222220002', 'Services', 'Consulting, labor, and professional services', TRUE),
    ('22222222-2222-2222-2222-222222220003', 'Hardware & Electronics', 'Electronic appliances, computer components, and hardware', TRUE);