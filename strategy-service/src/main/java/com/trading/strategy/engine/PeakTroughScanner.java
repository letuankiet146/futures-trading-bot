package com.trading.strategy.engine;

import com.trading.strategy.model.Candle;
import com.trading.strategy.model.SwingPoint;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Swing highs/lows from candle wicks: peaks compare {@link Candle#getHigh()}, troughs {@link Candle#getLow()}. */
@Component
public class PeakTroughScanner {
    public List<SwingPoint> findPeaks(List<Candle> candles, int k) {
        List<SwingPoint> peaks = new ArrayList<>();
        int n = candles.size();
        for (int i = k; i <= n - 1 - k; i++) {
            double center = candles.get(i).getHigh();
            boolean isPeak = true;
            for (int j = i - k; j <= i + k; j++) {
                if (j == i) {
                    continue;
                }
                if (center <= candles.get(j).getHigh()) {
                    isPeak = false;
                    break;
                }
            }
            if (isPeak) {
                peaks.add(new SwingPoint(i, candles.get(i).getCloseTime(), center));
            }
        }
        return peaks;
    }

    public List<SwingPoint> findTroughs(List<Candle> candles, int k) {
        List<SwingPoint> troughs = new ArrayList<>();
        int n = candles.size();
        for (int i = k; i <= n - 1 - k; i++) {
            double center = candles.get(i).getLow();
            boolean isTrough = true;
            for (int j = i - k; j <= i + k; j++) {
                if (j == i) {
                    continue;
                }
                if (center >= candles.get(j).getLow()) {
                    isTrough = false;
                    break;
                }
            }
            if (isTrough) {
                troughs.add(new SwingPoint(i, candles.get(i).getCloseTime(), center));
            }
        }
        return troughs;
    }
}
