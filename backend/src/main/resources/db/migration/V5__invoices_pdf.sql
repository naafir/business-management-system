-- ==============================================================================
-- Flyway Migration V5: Invoices & Server-Side PDF Generator Engine
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Invoices Table (Immutable GST Tax Invoices)
CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    sale_id UUID REFERENCES sales(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE RESTRICT,
    customer_name VARCHAR(200) NOT NULL,
    customer_gstin VARCHAR(15),
    place_of_supply_state VARCHAR(100) NOT NULL,
    place_of_supply_code VARCHAR(2) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, FINALIZED, CANCELLED
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
    payment_method VARCHAR(30), -- CASH, UPI, BANK_TRANSFER, CARD, CHEQUE, CREDIT
    notes TEXT,
    terms_and_conditions TEXT,
    created_by UUID REFERENCES users(id),
    created_by_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoices_sale ON invoices(sale_id);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_date ON invoices(invoice_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);

-- 2. Invoice Line Items Table
CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
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

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_product ON invoice_items(product_id);
