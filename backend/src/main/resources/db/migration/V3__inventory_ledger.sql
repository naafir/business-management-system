-- ==============================================================================
-- Flyway Migration V3: Inventory Transaction Ledger & Food Essence Seeds
-- Multi-database compatible (PostgreSQL 13+ & H2 PostgreSQL Mode)
-- ==============================================================================

-- 1. Inventory Transactions Ledger
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL, -- PURCHASE, SALE, RETURN_IN, RETURN_OUT, ADJUSTMENT, OPENING_STOCK
    quantity NUMERIC(15,2) NOT NULL,
    previous_stock NUMERIC(15,2) NOT NULL,
    new_stock NUMERIC(15,2) NOT NULL,
    unit_cost NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_cost NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    reference_type VARCHAR(50), -- MANUAL_ADJUSTMENT, INITIAL_OPENING, PURCHASE_BILL, TAX_INVOICE, CREDIT_NOTE
    reference_id UUID,
    reference_number VARCHAR(100),
    notes TEXT,
    created_by UUID REFERENCES users(id),
    created_by_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inv_trans_product ON inventory_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_trans_type ON inventory_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_inv_trans_created_at ON inventory_transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inv_trans_ref ON inventory_transactions(reference_type, reference_id);

-- 2. Seed Dedicated Categories for Essence & Food Materials Business
INSERT INTO product_categories (id, name, description, active)
SELECT '22222222-2222-2222-2222-222222220004', 'Food Essences & Flavors', 'Concentrated culinary essences, food aromas, and extract liquids', TRUE
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE name = 'Food Essences & Flavors');

INSERT INTO product_categories (id, name, description, active)
SELECT '22222222-2222-2222-2222-222222220005', 'Sauces & Condiments', 'Cooking sauces, chili, soya, vinegar, dips, and bottled dressings', TRUE
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE name = 'Sauces & Condiments');

INSERT INTO product_categories (id, name, description, active)
SELECT '22222222-2222-2222-2222-222222220006', 'Noodles & Pasta', 'Dry noodles, instant ramen, vermicelli, and macaroni packs', TRUE
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE name = 'Noodles & Pasta');

INSERT INTO product_categories (id, name, description, active)
SELECT '22222222-2222-2222-2222-222222220007', 'Spices & Seasonings', 'Gourmet spice mixes, flavor enhancers, and seasoning powders', TRUE
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE name = 'Spices & Seasonings');

INSERT INTO product_categories (id, name, description, active)
SELECT '22222222-2222-2222-2222-222222220008', 'Packaging & Containers', 'Bottles, pouches, carton boxes, and sealing materials', TRUE
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE name = 'Packaging & Containers');

-- 3. Backfill initial OPENING_STOCK ledger rows for any products with opening_stock > 0
INSERT INTO inventory_transactions (
    id, product_id, transaction_type, quantity, previous_stock, new_stock, unit_cost, total_cost, reference_type, reference_number, notes, created_at
)
SELECT 
    p.id, 
    p.id, 
    'OPENING_STOCK', 
    p.opening_stock, 
    0.00, 
    p.opening_stock, 
    p.purchase_price, 
    (p.opening_stock * p.purchase_price), 
    'INITIAL_OPENING', 
    'OPENING-BAL', 
    'Initial opening balance backfill', 
    p.created_at
FROM products p
WHERE p.opening_stock > 0
  AND NOT EXISTS (
      SELECT 1 FROM inventory_transactions it 
      WHERE it.product_id = p.id AND it.transaction_type = 'OPENING_STOCK'
  );
