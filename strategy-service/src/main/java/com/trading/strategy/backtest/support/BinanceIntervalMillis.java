package com.trading.strategy.backtest.support;

import com.trading.contracts.util.IntervalMillis;

/** Binance kline interval string → step length in milliseconds (open-to-open). */
public final class BinanceIntervalMillis {

    private BinanceIntervalMillis() {}

    public static long parse(String interval) {
        return IntervalMillis.parse(interval);
    }
}
