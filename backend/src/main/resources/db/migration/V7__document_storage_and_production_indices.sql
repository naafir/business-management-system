-- ==============================================================================
-- V7: Document Storage Subsystem & Production Query Performance Indices
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    notes TEXT,
    uploaded_by UUID,
    uploaded_by_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_entity ON documents(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);

-- Composite query performance indices
CREATE INDEX IF NOT EXISTS idx_sales_cust_date ON sales(customer_id, sale_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchases_supp_date ON purchases(supplier_id, purchase_date DESC);

