package com.trading.strategy.backtest;

/** Stable key for in-flight job deduplication (must match spec: absent endDate vs present). */
public final class BacktestDedupeKeys {

    private static final String END_OMITTED = "__END_OMITTED__";

    private BacktestDedupeKeys() {}

    private static final String TP_DEFAULT = "__TP_DEFAULT__";
    private static final String SL_DEFAULT = "__SL_DEFAULT__";

    public static String build(
            String symbol,
            String klineInterval,
            String requestStartRaw,
            String requestEndRaw,
            Double requestTpPercent,
            Double requestSlPercent) {
        String safeSymbol = sanitize(symbol);
        String safeInterval = sanitize(klineInterval);
        String safeStartRaw = sanitize(requestStartRaw);
        String safeEndRaw = sanitize(requestEndRaw);
        String endPart = (safeEndRaw == null || safeEndRaw.isBlank()) ? END_OMITTED : safeEndRaw;
        String tpPart = requestTpPercent != null ? Double.toString(requestTpPercent) : TP_DEFAULT;
        String slPart = requestSlPercent != null ? Double.toString(requestSlPercent) : SL_DEFAULT;
        return safeSymbol + "\t" + safeInterval + "\t" + safeStartRaw + "\t" + endPart + "\t" + tpPart + "\t" + slPart;
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replace("\u0000", "");
    }
}
