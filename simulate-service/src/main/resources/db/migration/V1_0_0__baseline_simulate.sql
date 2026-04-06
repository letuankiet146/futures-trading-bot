CREATE TABLE IF NOT EXISTS paper_account_snapshot (
    id BIGINT PRIMARY KEY,
    balance_usdt DOUBLE PRECISION NOT NULL,
    last_mark_price DOUBLE PRECISION,
    frozen BOOLEAN NOT NULL,
    win_count INTEGER NOT NULL,
    lose_count INTEGER NOT NULL,
    liquidation_count INTEGER NOT NULL,
    total_trades INTEGER NOT NULL,
    total_pnl DOUBLE PRECISION NOT NULL,
    total_fees DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS positions (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    entry_price DOUBLE PRECISION NOT NULL,
    take_profit_price DOUBLE PRECISION NOT NULL,
    stop_loss_price DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    opened_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS fills (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    fill_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    message VARCHAR(1000) NOT NULL
);
