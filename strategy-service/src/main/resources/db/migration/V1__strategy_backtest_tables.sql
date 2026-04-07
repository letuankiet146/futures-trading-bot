CREATE TABLE backtest_kline (
    symbol         VARCHAR(32) NOT NULL,
    kline_interval VARCHAR(16) NOT NULL,
    open_time_ms   BIGINT      NOT NULL,
    open_price     DOUBLE PRECISION NOT NULL,
    high_price     DOUBLE PRECISION NOT NULL,
    low_price      DOUBLE PRECISION NOT NULL,
    close_price    DOUBLE PRECISION NOT NULL,
    close_time_ms  BIGINT      NOT NULL,
    PRIMARY KEY (symbol, kline_interval, open_time_ms)
);

CREATE TABLE backtest_job (
    id                 UUID PRIMARY KEY,
    status             VARCHAR(32) NOT NULL,
    symbol             VARCHAR(32) NOT NULL,
    kline_interval     VARCHAR(16) NOT NULL,
    request_start_raw  TEXT        NOT NULL,
    request_end_raw    TEXT,
    dedupe_key         VARCHAR(768) NOT NULL,
    effective_start_ms BIGINT,
    effective_end_ms   BIGINT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    error_message      TEXT,
    candles_replayed   INTEGER
);

CREATE UNIQUE INDEX ux_backtest_job_dedupe_active
    ON backtest_job (dedupe_key)
    WHERE status IN ('PENDING', 'RUNNING');
