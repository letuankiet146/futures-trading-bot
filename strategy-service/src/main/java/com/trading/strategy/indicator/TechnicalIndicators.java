package com.trading.strategy.indicator;

import java.util.List;

public final class TechnicalIndicators {
    private TechnicalIndicators() {}

    public static double ema(List<Double> values, int period) {
        if (values == null || values.size() < period || period <= 0) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            sum += values.get(i);
        }
        double ema = sum / period;
        double multiplier = 2.0 / (period + 1.0);
        for (int i = period; i < values.size(); i++) {
            ema = (values.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    public static double rsi(List<Double> closes, int period) {
        if (closes == null || closes.size() <= period || period <= 0) {
            return Double.NaN;
        }
        double avgGain = 0.0;
        double avgLoss = 0.0;
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss -= change;
            }
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            double gain = change > 0 ? change : 0.0;
            double loss = change < 0 ? -change : 0.0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        if (avgLoss == 0.0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    public static double average(List<Double> values, int lookback, int endExclusive) {
        if (values == null || lookback <= 0 || endExclusive <= 0 || endExclusive > values.size()) {
            return Double.NaN;
        }
        int start = Math.max(0, endExclusive - lookback);
        if (endExclusive - start < lookback) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = start; i < endExclusive; i++) {
            sum += values.get(i);
        }
        return sum / lookback;
    }
}
