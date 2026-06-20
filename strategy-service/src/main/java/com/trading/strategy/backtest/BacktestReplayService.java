package com.trading.strategy.backtest;

import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.backtest.BacktestExitOverrides;
import com.trading.strategy.engine.ExitBracketCalculator;
import com.trading.strategy.engine.ExitDistanceResolver;
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
    private final StrategySignalEvaluator evaluator;
    private final BacktestSimulateFeedPublisher simulateFeedPublisher;

    public BacktestReplayService(
            StrategyProperties strategyProperties,
            StrategySignalEvaluator evaluator,
            BacktestSimulateFeedPublisher simulateFeedPublisher) {
        this.strategyProperties = strategyProperties;
        this.evaluator = evaluator;
        this.simulateFeedPublisher = simulateFeedPublisher;
    }

    /** @return number of strategy signals emitted */
    public int replay(List<Candle> candles, String correlationId) {
        return replay(candles, correlationId, BacktestExitOverrides.none());
    }

    /** @return number of strategy signals emitted */
    public int replay(List<Candle> candles, String correlationId, BacktestExitOverrides exitOverrides) {
        BacktestExitOverrides overrides = exitOverrides == null ? BacktestExitOverrides.none() : exitOverrides;
        String replayCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        // Isolate each job: reset the paper account to its initial balance before replaying this job's feed.
        String resetTimestamp = candles.isEmpty()
                ? Instant.now().toString()
                : Instant.ofEpochMilli(candles.get(0).getOpenTime()).toString();
        simulateFeedPublisher.publishReset(strategyProperties.getSymbol(), replayCorrelationId, resetTimestamp);
        simulateFeedPublisher.flush();
        int minCandles = Math.max(strategyProperties.getN(), 2 * strategyProperties.getK() + 2);
        if (candles.size() < minCandles) {
            log.warn("Not enough candles for backtest replay: need at least {} have {}", minCandles, candles.size());
            return 0;
        }
        int windowN = strategyProperties.getN();
        int emitted = 0;
        // Strategy is evaluated once per closed candle. A raised signal is deferred and executed on the
        // next candle's open, so the backtest never trades on a candle whose close it could not have seen yet.
        boolean pendingSignal = false;
        StrategyDecision pendingDecision = null;
        for (int candleIdx = 0; candleIdx < candles.size(); candleIdx++) {
            Candle candle = candles.get(candleIdx);
            int windowStart = Math.max(0, candleIdx - windowN + 1);
            List<Candle> history = candles.subList(windowStart, candleIdx + 1);
            if (history.size() < windowN) {
                continue;
            }

            // Execute a signal raised at the previous candle's close, filling at this candle's open.
            if (pendingSignal && pendingDecision != null) {
                double entry = candle.getOpen();
                double[] bracket = ExitBracketCalculator.recenterAtEntry(
                        pendingDecision.side(),
                        pendingDecision.signalPrice(),
                        entry,
                        pendingDecision.takeProfitPrice(),
                        pendingDecision.stopLossPrice());
                String openTimestamp = toReplayTimestamp(candle, 0, 4);
                simulateFeedPublisher.publishSignal(
                        strategyProperties.getSymbol(),
                        pendingDecision.side(),
                        entry,
                        bracket[0],
                        bracket[1],
                        replayCorrelationId,
                        openTimestamp);
                simulateFeedPublisher.flush();
                emitted++;
                pendingSignal = false;
                pendingDecision = null;
            }

            // Emit one CANDLE so simulate-service can resolve intra-candle TP/SL order by drilling down.
            simulateFeedPublisher.publishCandle(
                    strategyProperties.getSymbol(),
                    strategyProperties.getInterval(),
                    candle.getOpenTime(),
                    candle.getCloseTime(),
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    replayCorrelationId,
                    toReplayTimestamp(candle, 0, 4));
            simulateFeedPublisher.flush();

            // Evaluate once at the candle close; any signal is deferred to the next candle.
            StrategyDecision decision = evaluator.evaluate(history, candle.getClose());
            if (decision.shouldSignal()) {
                pendingSignal = true;
                pendingDecision = applyExitOverrides(decision, overrides);
            }
        }
        if (pendingSignal && pendingDecision != null) {
            log.info(
                    "Replay ended with a pending {} signal but no next candle to execute it on",
                    pendingDecision.side());
        }
        log.info(
                "Backtest replay completed candles={} emittedSignals={} symbol={} interval={}",
                candles.size(),
                emitted,
                strategyProperties.getSymbol(),
                strategyProperties.getInterval());
        return emitted;
    }

    private StrategyDecision applyExitOverrides(StrategyDecision decision, BacktestExitOverrides overrides) {
        if (!decision.shouldSignal() || !overrides.hasAny()) {
            return decision;
        }
        double tpRate = overrides.tpPercent() != null
                ? overrides.tpPercent()
                : ExitDistanceResolver.tpDistanceRate(strategyProperties);
        double slRate = overrides.slPercent() != null
                ? overrides.slPercent()
                : ExitDistanceResolver.slDistanceRate(strategyProperties);
        double[] bracket = ExitBracketCalculator.percentBracket(
                decision.side(), decision.signalPrice(), tpRate, slRate);
        return new StrategyDecision(
                decision.shouldSignal(),
                decision.side(),
                decision.avgTop(),
                decision.avgBottom(),
                decision.signalPrice(),
                bracket[0],
                bracket[1]);
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
