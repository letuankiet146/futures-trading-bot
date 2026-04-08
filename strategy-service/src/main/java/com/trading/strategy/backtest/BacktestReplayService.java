package com.trading.strategy.backtest;

import com.trading.strategy.config.BacktestProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.engine.StrategySignalEvaluator;
import com.trading.strategy.kafka.BacktestSimulateFeedPublisher;
import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BacktestReplayService {

    private static final Logger log = LoggerFactory.getLogger(BacktestReplayService.class);

    private final StrategyProperties strategyProperties;
    private final BacktestProperties backtestProperties;
    private final StrategySignalEvaluator evaluator;
    private final BacktestSimulateFeedPublisher simulateFeedPublisher;

    public BacktestReplayService(
            StrategyProperties strategyProperties,
            BacktestProperties backtestProperties,
            StrategySignalEvaluator evaluator,
            BacktestSimulateFeedPublisher simulateFeedPublisher) {
        this.strategyProperties = strategyProperties;
        this.backtestProperties = backtestProperties;
        this.evaluator = evaluator;
        this.simulateFeedPublisher = simulateFeedPublisher;
    }

    /** @return number of strategy signals emitted */
    public int replay(List<Candle> candles, String correlationId) {
        String replayCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        int minCandles = 2 * strategyProperties.getK() + 2;
        if (candles.size() < minCandles) {
            log.warn("Not enough candles for backtest replay: {}", candles.size());
            return 0;
        }
        int windowN = strategyProperties.getN();
        int emitted = 0;
        for (int candleIdx = 0; candleIdx < candles.size(); candleIdx++) {
            Candle candle = candles.get(candleIdx);
            int windowStart = Math.max(0, candleIdx - windowN + 1);
            List<Candle> history = candles.subList(windowStart, candleIdx + 1);
            if (history.size() < (2 * strategyProperties.getK() + 1)) {
                continue;
            }
            List<Double> path = toPricePath(candle);
            for (int i = 0; i < path.size(); i++) {
                double mark = path.get(i);
                String eventTimestamp = toReplayTimestamp(candle, i, path.size());
                StrategyDecision decision = evaluator.evaluate(history, mark);
                simulateFeedPublisher.publishMark(
                        strategyProperties.getSymbol(), mark, replayCorrelationId, eventTimestamp);
                if (decision.shouldSignal()) {
                    simulateFeedPublisher.publishSignal(
                            strategyProperties.getSymbol(), decision.side(), mark, replayCorrelationId, eventTimestamp);
                    emitted++;
                }
                simulateFeedPublisher.flush();
            }
        }
        log.info(
                "Backtest replay completed candles={} emittedSignals={} symbol={} interval={}",
                candles.size(),
                emitted,
                strategyProperties.getSymbol(),
                strategyProperties.getInterval());
        return emitted;
    }

    private List<Double> toPricePath(Candle candle) {
        String order =
                backtestProperties.getOhlcOrder() == null ? "OHLC" : backtestProperties.getOhlcOrder().toUpperCase();
        if ("OLHC".equals(order)) {
            return List.of(candle.getOpen(), candle.getLow(), candle.getHigh(), candle.getClose());
        }
        return List.of(candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose());
    }

    private String toReplayTimestamp(Candle candle, int stepIndex, int totalSteps) {
        long start = candle.getOpenTime();
        long end = candle.getCloseTime();
        if (totalSteps <= 1 || end <= start) {
            return Instant.ofEpochMilli(start).toString();
        }
        long offset = ((end - start) * stepIndex) / (totalSteps - 1);
        return Instant.ofEpochMilli(start + offset).toString();
    }
}
