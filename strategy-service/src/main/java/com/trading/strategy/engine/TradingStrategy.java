package com.trading.strategy.engine;

import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.util.List;

public interface TradingStrategy {
    StrategyDecision evaluate(List<Candle> closedCandles, double markPrice);
}
