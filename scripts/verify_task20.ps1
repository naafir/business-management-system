# Verification Script for Task 20
$ErrorActionPreference = 'Stop'
$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$db = 'business_billing_db'

Write-Host "=== POST-MIGRATION VERIFICATION: 1. Documents & Payments Tables ==="
& $psql -h localhost -p 5432 -U postgres -d $db -c "\d documents"
& $psql -h localhost -p 5432 -U postgres -d $db -c "\d payments"

Write-Host "`n=== POST-MIGRATION VERIFICATION: 2. Invoices Seller Snapshot Columns ==="
& $psql -h localhost -p 5432 -U postgres -d $db -c "
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name='invoices' AND column_name LIKE 'seller_%' 
ORDER BY ordinal_position;
"

Write-Host "`n=== POST-MIGRATION VERIFICATION: 3. Indexes Added in V7, V8, V9, V10 ==="
& $psql -h localhost -p 5432 -U postgres -d $db -c "
SELECT indexname, tablename, indexdef 
FROM pg_indexes 
WHERE indexname IN (
    'idx_documents_entity', 'idx_documents_created_at',
    'idx_sales_cust_date', 'idx_purchases_supp_date',
    'idx_payments_invoice_id', 'idx_payments_sale_id', 'idx_payments_customer_id', 'idx_payments_payment_date',
    'idx_invoices_sale_id', 'idx_invoices_status',
    'idx_sale_items_hsn', 'idx_sales_gstin_date'
)
ORDER BY tablename, indexname;
"

Write-Host "`n=== POST-MIGRATION VERIFICATION: 4. Post-Migration Row Counts ==="
& $psql -h localhost -p 5432 -U postgres -d $db -c "
SELECT 'users' AS tbl, count(*) FROM users
UNION ALL SELECT 'business_settings', count(*) FROM business_settings
UNION ALL SELECT 'gst_rates', count(*) FROM gst_rates
UNION ALL SELECT 'product_categories', count(*) FROM product_categories
UNION ALL SELECT 'expense_categories', count(*) FROM expense_categories
UNION ALL SELECT 'products', count(*) FROM products
UNION ALL SELECT 'inventory_transactions', count(*) FROM inventory_transactions
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
UNION ALL SELECT 'backup_records', count(*) FROM backup_records
UNION ALL SELECT 'customers', count(*) FROM customers
UNION ALL SELECT 'suppliers', count(*) FROM suppliers
UNION ALL SELECT 'purchases', count(*) FROM purchases
UNION ALL SELECT 'purchase_items', count(*) FROM purchase_items
UNION ALL SELECT 'sales', count(*) FROM sales
UNION ALL SELECT 'sale_items', count(*) FROM sale_items
UNION ALL SELECT 'invoices', count(*) FROM invoices
UNION ALL SELECT 'invoice_items', count(*) FROM invoice_items
UNION ALL SELECT 'expenses', count(*) FROM expenses
UNION ALL SELECT 'documents', count(*) FROM documents
UNION ALL SELECT 'payments', count(*) FROM payments
UNION ALL SELECT 'flyway_schema_history', count(*) FROM flyway_schema_history;
"

Write-Host "`n=== POST-MIGRATION VERIFICATION: 5. Exact Seed Data Preserved ==="
& $psql -h localhost -p 5432 -U postgres -d $db -c "SELECT id, username, role, active FROM users;"
& $psql -h localhost -p 5432 -U postgres -d $db -c "SELECT id, name, sku, current_stock FROM products;"
& $psql -h localhost -p 5432 -U postgres -d $db -c "SELECT id, transaction_type, quantity FROM inventory_transactions;"
& $psql -h localhost -p 5432 -U postgres -d $db -c "SELECT id, legal_name, gstin FROM business_settings;"
& $psql -h localhost -p 5432 -U postgres -d $db -c "SELECT id, action, entity_type FROM audit_logs ORDER BY created_at;"
