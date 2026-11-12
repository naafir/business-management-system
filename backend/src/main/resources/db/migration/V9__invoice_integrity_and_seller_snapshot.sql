-- ==============================================================================
-- Flyway Invoice Integrity & Historical Seller Snapshot Migration (V9)
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_legal_name VARCHAR(200);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_trade_name VARCHAR(200);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_gstin VARCHAR(15);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_address_line1 VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_address_line2 VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_city VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_state_name VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_state_code VARCHAR(2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_pin_code VARCHAR(10);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_phone VARCHAR(20);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_email VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_bank_name VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_bank_account_number VARCHAR(50);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_bank_ifsc VARCHAR(20);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_bank_branch VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_bank_upi_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_sale_id ON invoices(sale_id);
