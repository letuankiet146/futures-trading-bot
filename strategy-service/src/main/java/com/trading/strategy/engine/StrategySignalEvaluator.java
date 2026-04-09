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
        int nWindow = strategyProperties.getN();
        int minBarsForSwing = 2 * strategyProperties.getK() + 1;
        int requiredSize = Math.max(nWindow, minBarsForSwing);
        if (closedCandles.size() < requiredSize) {
            return new StrategyDecision(false, "NONE", 0.0, 0.0, 0.0);
        }

        // 1) Full N-bar window: scan every peak / trough in range, then 2) average drawdowns over those sets.
        List<SwingPoint> peaks = scanner.findPeaks(closedCandles, strategyProperties.getK());
        List<SwingPoint> troughs = scanner.findTroughs(closedCandles, strategyProperties.getK());

        double avgTop = peaks.isEmpty()
                ? 0.0
                : peaks.stream().mapToDouble(p -> (p.price() - markPrice) / p.price()).average().orElse(0.0);
        double avgBottom = troughs.isEmpty()
                ? 0.0
                : troughs.stream().mapToDouble(t -> (markPrice - t.price()) / t.price()).average().orElse(0.0);

        double diff = Math.abs(avgTop - avgBottom);
        double feeGate = strategyProperties.getFeeGateMultiplier() * strategyProperties.getTakerFee();
        boolean isMiddle = diff <= strategyProperties.getSimilarityThreshold();
        boolean feeEnough = Math.max(avgTop, avgBottom) > feeGate;

        if (!isMiddle || !feeEnough || peaks.isEmpty() || troughs.isEmpty()) {
            return new StrategyDecision(false, "NONE", avgTop, avgBottom, 0.0);
        }

        SwingPoint latestPeak = peaks.get(peaks.size() - 1);
        SwingPoint latestTrough = troughs.get(troughs.size() - 1);
        String side = latestPeak.index() > latestTrough.index() ? "SELL" : "BUY";

        double lastClose = closedCandles.get(closedCandles.size() - 1).getClose();
        return new StrategyDecision(true, side, avgTop, avgBottom, lastClose);
    }
}
