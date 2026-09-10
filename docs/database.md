# Database Design & Schema Conventions

## Key Rules
1. **Primary Keys**: UUIDs generated on the server or database via `gen_random_uuid()`.
2. **Numeric Fields**: Currency amounts are `NUMERIC(15, 2)`. Quantities are `NUMERIC(15, 2)` or `NUMERIC(15, 4)` for fractional units.
3. **Temporal Tracking**: Every business entity inherits `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP` and `updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`.
4. **Soft Deletion**: Entities maintain an `active` boolean or status enum (`ACTIVE`, `INACTIVE`, `CANCELLED`, `ARCHIVED`). Hard deletes (`DELETE FROM ...`) are rejected for core domain records.
5. **Historical Snapshots**: Line items on invoices capture full textual copies of product descriptions, HSN/SAC codes, and GST rates at the instant of invoice creation.
