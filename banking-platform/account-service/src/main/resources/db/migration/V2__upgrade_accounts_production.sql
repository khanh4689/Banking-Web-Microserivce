-- V2: Production-level upgrades to accounts table

-- 1. Fix balance precision: scale 4 → 2 (cent-level, e.g. 0.00)
ALTER TABLE accounts ALTER COLUMN balance TYPE NUMERIC(19, 2);

-- 2. Add currency column (ISO 4217, default VND for existing rows)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'VND';

-- 3. Add optimistic locking version column (starts at 0)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 4. Ensure updated_at is populated for existing rows
UPDATE accounts SET updated_at = created_at WHERE updated_at IS NULL;
