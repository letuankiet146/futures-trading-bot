ALTER TABLE strategy.backtest_job
    ADD COLUMN sim_balance_usdt DOUBLE PRECISION,
    ADD COLUMN sim_last_mark_price DOUBLE PRECISION,
    ADD COLUMN sim_frozen BOOLEAN,
    ADD COLUMN sim_total_trades INTEGER,
    ADD COLUMN sim_win_count INTEGER,
    ADD COLUMN sim_lose_count INTEGER,
    ADD COLUMN sim_liquidation_count INTEGER,
    ADD COLUMN sim_total_pnl DOUBLE PRECISION,
    ADD COLUMN sim_total_fees DOUBLE PRECISION,
    ADD COLUMN sim_open_position_active BOOLEAN;
