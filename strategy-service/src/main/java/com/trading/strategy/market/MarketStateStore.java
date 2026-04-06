package com.trading.strategy.market;

import com.trading.strategy.model.Candle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class MarketStateStore {
    private final List<Candle> closedCandles = new ArrayList<>();
    private final AtomicReference<Double> markPrice = new AtomicReference<>();
    private final Object lock = new Object();

    public void replaceClosedCandles(List<Candle> candles, int maxSize) {
        synchronized (lock) {
            closedCandles.clear();
            int from = Math.max(0, candles.size() - maxSize);
            closedCandles.addAll(candles.subList(from, candles.size()));
        }
    }

    public void upsertFromWs(Candle wsCandle, int maxSize) {
        synchronized (lock) {
            if (!wsCandle.isClosed()) {
                return;
            }
            if (!closedCandles.isEmpty() && closedCandles.get(closedCandles.size() - 1).getOpenTime() == wsCandle.getOpenTime()) {
                closedCandles.set(closedCandles.size() - 1, wsCandle);
            } else {
                closedCandles.add(wsCandle);
            }
            while (closedCandles.size() > maxSize) {
                closedCandles.remove(0);
            }
        }
    }

    public List<Candle> snapshotClosedCandles() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(closedCandles));
        }
    }

    public Double getMarkPrice() {
        return markPrice.get();
    }

    public void setMarkPrice(double value) {
        markPrice.set(value);
    }
}
