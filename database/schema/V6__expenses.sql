-- V6: Expense Categories and Expenses Table
-- Phase 7: Expense Tracking

CREATE TABLE expense_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES expense_categories(id),
    expense_date DATE NOT NULL,
    vendor_name VARCHAR(200),
    description TEXT NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    gst_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    is_gst_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method VARCHAR(30),
    reference_number VARCHAR(100),
    notes TEXT,
    created_by UUID,
    created_by_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed default expense categories
INSERT INTO expense_categories (id, name, description) VALUES
    (gen_random_uuid(), 'Rent & Utilities', 'Office/shop rent, electricity, water bills'),
    (gen_random_uuid(), 'Salaries & Wages', 'Employee salaries, contract labour payments'),
    (gen_random_uuid(), 'Transport & Logistics', 'Freight, courier, delivery charges'),
    (gen_random_uuid(), 'Marketing & Advertising', 'Ads, promotions, printed materials'),
    (gen_random_uuid(), 'Office Supplies', 'Stationery, printer ink, misc office items'),
    (gen_random_uuid(), 'Repairs & Maintenance', 'Equipment servicing, building maintenance'),
    (gen_random_uuid(), 'Professional Fees', 'CA, lawyer, consultant fees'),
    (gen_random_uuid(), 'Bank Charges', 'Bank fees, payment gateway charges'),
    (gen_random_uuid(), 'Miscellaneous', 'Other operational expenses');

CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expenses_category ON expenses(category_id);
