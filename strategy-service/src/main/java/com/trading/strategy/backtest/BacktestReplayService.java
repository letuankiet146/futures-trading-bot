package com.trading.strategy.backtest;

import com.trading.strategy.config.BacktestProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.engine.StrategySignalEvaluator;
import com.trading.strategy.kafka.BacktestSimulateFeedPublisher;
import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.util.ArrayList;
import java.util.List;
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
    public int replay(List<Candle> candles) {
        if (candles.size() < (2 * strategyProperties.getK() + 2)) {
            log.warn("Not enough candles for backtest replay: {}", candles.size());
            return 0;
        }
        int emitted = 0;
        List<Candle> history = new ArrayList<>();
        for (Candle candle : candles) {
            history.add(candle);
            if (history.size() < (2 * strategyProperties.getK() + 1)) {
                continue;
            }
            for (double mark : toPricePath(candle)) {
                StrategyDecision decision = evaluator.evaluate(history, mark);
                simulateFeedPublisher.publishMark(strategyProperties.getSymbol(), mark);
                if (decision.shouldSignal()) {
                    simulateFeedPublisher.publishSignal(strategyProperties.getSymbol(), decision.side(), mark);
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
}
