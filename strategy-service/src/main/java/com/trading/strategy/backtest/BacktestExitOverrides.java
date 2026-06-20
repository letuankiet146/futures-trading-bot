package com.trading.strategy.backtest;

/** Optional per-job TP/SL percent overrides (decimal rate, e.g. 0.5 = 50% price move). */
public record BacktestExitOverrides(Double tpPercent, Double slPercent) {

    public static BacktestExitOverrides none() {
        return new BacktestExitOverrides(null, null);
    }

    public boolean hasAny() {
        return tpPercent != null || slPercent != null;
    }
}
