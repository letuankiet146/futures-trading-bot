package com.trading.strategy.backtest;

/**
 * Stored in {@code backtest_job.request_start_raw} when the client omits both {@code startDate} and {@code endDate}
 * — job replays the latest 1500 fully closed klines (Binance REST {@code limit=1500}).
 */
public final class BacktestRequestSentinels {

    public static final String LAST_1500_KLINES = "__LAST_1500_KLINES__";
    public static final int LAST_1500_COUNT = 1500;

    private BacktestRequestSentinels() {}

    public static boolean isLast1500KlinesRequest(String requestStartRaw) {
        return LAST_1500_KLINES.equals(requestStartRaw);
    }
}
