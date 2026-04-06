package com.trading.strategy.engine;

import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import com.trading.strategy.model.SwingPoint;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StrategySignalEvaluator {
    private final PeakTroughScanner scanner;
    private final StrategyProperties strategyProperties;

    public StrategySignalEvaluator(PeakTroughScanner scanner, StrategyProperties strategyProperties) {
        this.scanner = scanner;
        this.strategyProperties = strategyProperties;
    }

    public StrategyDecision evaluate(List<Candle> closedCandles, double markPrice) {
        if (closedCandles.size() < (2 * strategyProperties.getK() + 1)) {
            return new StrategyDecision(false, "NONE", 0.0, 0.0);
        }

        List<SwingPoint> peaks = scanner.findPeaks(closedCandles, strategyProperties.getK());
        List<SwingPoint> troughs = scanner.findTroughs(closedCandles, strategyProperties.getK());

        double avgTop = peaks.isEmpty()
                ? 0.0
                : peaks.stream().mapToDouble(p -> (p.price() - markPrice) / p.price()).average().orElse(0.0);
        double avgBottom = troughs.isEmpty()
                ? 0.0
                : troughs.stream().mapToDouble(t -> (markPrice - t.price()) / t.price()).average().orElse(0.0);

        double diff = Math.abs(avgTop - avgBottom);
        double feeGate = 4.0 * strategyProperties.getTakerFee();
        boolean isMiddle = diff <= strategyProperties.getSimilarityThreshold();
        boolean feeEnough = Math.max(avgTop, avgBottom) > feeGate;

        if (!isMiddle || !feeEnough || peaks.isEmpty() || troughs.isEmpty()) {
            return new StrategyDecision(false, "NONE", avgTop, avgBottom);
        }

        SwingPoint latestPeak = peaks.get(peaks.size() - 1);
        SwingPoint latestTrough = troughs.get(troughs.size() - 1);
        String side = latestPeak.index() > latestTrough.index() ? "SELL" : "BUY";

        return new StrategyDecision(true, side, avgTop, avgBottom);
    }
}
