ALTER TABLE positions
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(128);
