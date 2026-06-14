ALTER TABLE backtest_kline
    ADD COLUMN quote_asset_volume       DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN taker_quote_asset_volume DOUBLE PRECISION NOT NULL DEFAULT 0;
