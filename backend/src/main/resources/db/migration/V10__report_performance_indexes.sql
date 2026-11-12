-- ==============================================================================
-- Flyway Migration V10: Report & Aggregation Query Performance Indexes
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Index on sale_items(hsn_sac) to accelerate HSN/SAC group-by reporting
CREATE INDEX IF NOT EXISTS idx_sale_items_hsn ON sale_items(hsn_sac);

-- 2. Composite index on sales(customer_gstin, sale_date) to accelerate B2B GST aggregate filtering
CREATE INDEX IF NOT EXISTS idx_sales_gstin_date ON sales(customer_gstin, sale_date);
