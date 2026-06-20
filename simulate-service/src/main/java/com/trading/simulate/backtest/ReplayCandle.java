package com.trading.simulate.backtest;

/** A single OHLC candle used by the intra-candle TP/SL drill-down during backtest replay. */
public record ReplayCandle(
        String interval, long openTimeMs, long closeTimeMs, double open, double high, double low, double close) {}
