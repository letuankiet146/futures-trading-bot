-- A non-positive configured TP/SL percent now means "no take-profit" / "no stop-loss",
-- so the persisted bracket prices may be absent.
ALTER TABLE positions
    ALTER COLUMN take_profit_price DROP NOT NULL;
ALTER TABLE positions
    ALTER COLUMN stop_loss_price DROP NOT NULL;
