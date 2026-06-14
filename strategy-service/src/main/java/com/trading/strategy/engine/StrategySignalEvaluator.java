package com.trading.strategy.engine;

import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StrategySignalEvaluator {
    private final TradingStrategy tradingStrategy;

    public StrategySignalEvaluator(TradingStrategy tradingStrategy) {
        this.tradingStrategy = tradingStrategy;
    }

    public StrategyDecision evaluate(List<Candle> closedCandles, double markPrice) {
        return tradingStrategy.evaluate(closedCandles, markPrice);
    }
}
