-- ==============================================================================
-- Flyway Migration V4: Purchases & Sales Commercial Transactions Engine
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Purchases Table (Inbound Vendor Invoices / Goods Received)
CREATE TABLE IF NOT EXISTS purchases (
    id UUID PRIMARY KEY,
    purchase_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    supplier_invoice_number VARCHAR(100),
    purchase_date DATE NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_discount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    amount_paid NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    balance_due NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PARTIALLY_PAID, PAID
    payment_method VARCHAR(30), -- CASH, BANK_TRANSFER, UPI, CHEQUE, CREDIT
    notes TEXT,
    created_by UUID REFERENCES users(id),
    created_by_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_purchases_number ON purchases(purchase_number);
CREATE INDEX IF NOT EXISTS idx_purchases_supplier ON purchases(supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases(purchase_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchases_payment_status ON purchases(payment_status);

-- 2. Purchase Line Items Table
CREATE TABLE IF NOT EXISTS purchase_items (
    id UUID PRIMARY KEY,
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(50),
    hsn_sac VARCHAR(20),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    quantity NUMERIC(15,2) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_amount NUMERIC(15,2) NOT NULL,
    gst_rate_percent NUMERIC(5,2) NOT NULL,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase ON purchase_items(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_product ON purchase_items(product_id);

-- 3. Sales Table (Outbound Commercial Orders / POS Sales)
CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY,
    sale_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID REFERENCES customers(id) ON DELETE RESTRICT,
    customer_name VARCHAR(200) NOT NULL,
    customer_gstin VARCHAR(15),
    place_of_supply_state VARCHAR(100) NOT NULL,
    place_of_supply_code VARCHAR(2) NOT NULL,
    sale_date DATE NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_discount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    amount_paid NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    balance_due NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID', -- PAID, PARTIALLY_PAID, PENDING
    payment_method VARCHAR(30), -- CASH, UPI, BANK_TRANSFER, CARD, CREDIT
    notes TEXT,
    created_by UUID REFERENCES users(id),
    created_by_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sales_number ON sales(sale_number);
CREATE INDEX IF NOT EXISTS idx_sales_customer ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_payment_status ON sales(payment_status);

-- 4. Sale Line Items Table
CREATE TABLE IF NOT EXISTS sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(50),
    hsn_sac VARCHAR(20),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    quantity NUMERIC(15,2) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_amount NUMERIC(15,2) NOT NULL,
    gst_rate_percent NUMERIC(5,2) NOT NULL,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product ON sale_items(product_id);
