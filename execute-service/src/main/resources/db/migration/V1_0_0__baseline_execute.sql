CREATE TABLE IF NOT EXISTS risk_bucket_state (
    id BIGINT PRIMARY KEY,
    bucket_start TIMESTAMP NOT NULL,
    opening_equity DOUBLE PRECISION NOT NULL,
    realized_loss DOUBLE PRECISION NOT NULL,
    paused BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    type VARCHAR(32) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    client_order_id VARCHAR(64),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS positions (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    entry_price DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    opened_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS fills (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    fill_type VARCHAR(64) NOT NULL,
    fill_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    message VARCHAR(1000) NOT NULL
);
