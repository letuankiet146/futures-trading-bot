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

    /**
     * Percent bracket where a non-positive rate disables that leg (returns {@code null} for it), meaning
     * "no take-profit" / "no stop-loss".
     */
    public static Double[] percentBracketNullable(String side, double entry, double tpPercent, double slPercent) {
        boolean buy = "BUY".equalsIgnoreCase(side);
        Double tp = tpPercent > 0 ? (buy ? entry * (1 + tpPercent) : entry * (1 - tpPercent)) : null;
        Double sl = slPercent > 0 ? (buy ? entry * (1 - slPercent) : entry * (1 + slPercent)) : null;
        return new Double[] {tp, sl};
    }

    /** Re-apply bracket distances onto a new entry, preserving disabled ({@code null}) legs. */
    public static Double[] recenterAtEntryNullable(
            String side, double referencePrice, double entryPrice, Double takeProfitPrice, Double stopLossPrice) {
        boolean buy = "BUY".equalsIgnoreCase(side);
        Double tp = null;
        Double sl = null;
        if (takeProfitPrice != null) {
            double tpRate = buy
                    ? (takeProfitPrice - referencePrice) / referencePrice
                    : (referencePrice - takeProfitPrice) / referencePrice;
            tp = buy ? entryPrice * (1 + tpRate) : entryPrice * (1 - tpRate);
        }
        if (stopLossPrice != null) {
            double slRate = buy
                    ? (referencePrice - stopLossPrice) / referencePrice
                    : (stopLossPrice - referencePrice) / referencePrice;
            sl = buy ? entryPrice * (1 - slRate) : entryPrice * (1 + slRate);
        }
        return new Double[] {tp, sl};
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
