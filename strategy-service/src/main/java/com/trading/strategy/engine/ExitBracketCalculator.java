package com.trading.strategy.engine;

/** Strategy-owned TP/SL price calculation (percent or peak-trough structure). */
public final class ExitBracketCalculator {
    private ExitBracketCalculator() {
    }

    public static double[] percentBracket(String side, double entry, double tpPercent, double slPercent) {
        if ("BUY".equalsIgnoreCase(side)) {
            return new double[] {entry * (1 + tpPercent), entry * (1 - slPercent)};
        }
        return new double[] {entry * (1 - tpPercent), entry * (1 + slPercent)};
    }

    public static double[] peakTroughBracket(String side, double entry, double avgTop, double avgBottom) {
        if ("BUY".equalsIgnoreCase(side)) {
            return new double[] {entry * (1 + avgTop), entry * (1 - avgBottom)};
        }
        return new double[] {entry * (1 - avgBottom), entry * (1 + avgTop)};
    }

    /** Re-apply bracket distances from reference price onto a new entry (backtest next-candle open). */
    public static double[] recenterAtEntry(
            String side, double referencePrice, double entryPrice, double takeProfitPrice, double stopLossPrice) {
        if ("BUY".equalsIgnoreCase(side)) {
            double tpRate = (takeProfitPrice - referencePrice) / referencePrice;
            double slRate = (referencePrice - stopLossPrice) / referencePrice;
            return new double[] {entryPrice * (1 + tpRate), entryPrice * (1 - slRate)};
        }
        double tpRate = (referencePrice - takeProfitPrice) / referencePrice;
        double slRate = (stopLossPrice - referencePrice) / referencePrice;
        return new double[] {entryPrice * (1 - tpRate), entryPrice * (1 + slRate)};
    }
}
