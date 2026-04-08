CREATE TABLE IF NOT EXISTS job_candle (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(128) NOT NULL,
    ts TIMESTAMP NOT NULL,
    open DOUBLE PRECISION NOT NULL,
    high DOUBLE PRECISION NOT NULL,
    low DOUBLE PRECISION NOT NULL,
    close DOUBLE PRECISION NOT NULL,
    volume DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_candle_job_id_ts ON job_candle (job_id, ts);

CREATE TABLE IF NOT EXISTS job_trade_event (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    tp DOUBLE PRECISION,
    sl DOUBLE PRECISION,
    event_time TIMESTAMP NOT NULL,
    metadata_json VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_job_trade_event_job_id_event_time ON job_trade_event (job_id, event_time);

CREATE TABLE IF NOT EXISTS job_balance_snapshot (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(128) NOT NULL,
    balance_usdt DOUBLE PRECISION NOT NULL,
    event_time TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_balance_snapshot_job_id_event_time ON job_balance_snapshot (job_id, event_time);
