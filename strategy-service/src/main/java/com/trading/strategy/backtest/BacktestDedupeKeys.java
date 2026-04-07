package com.trading.strategy.backtest;

/** Stable key for in-flight job deduplication (must match spec: absent endDate vs present). */
public final class BacktestDedupeKeys {

    private static final String END_OMITTED = "__END_OMITTED__";

    private BacktestDedupeKeys() {}

    public static String build(String symbol, String klineInterval, String requestStartRaw, String requestEndRaw) {
        String safeSymbol = sanitize(symbol);
        String safeInterval = sanitize(klineInterval);
        String safeStartRaw = sanitize(requestStartRaw);
        String safeEndRaw = sanitize(requestEndRaw);
        String endPart = (safeEndRaw == null || safeEndRaw.isBlank()) ? END_OMITTED : safeEndRaw;
        return safeSymbol + "\t" + safeInterval + "\t" + safeStartRaw + "\t" + endPart;
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replace("\u0000", "");
    }
}
