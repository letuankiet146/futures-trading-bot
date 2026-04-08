package com.trading.strategy.engine;

import com.trading.strategy.config.BacktestProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.kafka.StrategySignalPublisher;
import com.trading.strategy.market.MarketStateStore;
import com.trading.strategy.model.StrategyDecision;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LiveSignalTrigger {
    private static final Logger log = LoggerFactory.getLogger(LiveSignalTrigger.class);

    private final MarketStateStore marketStateStore;
    private final StrategySignalEvaluator evaluator;
    private final StrategySignalPublisher publisher;
    private final StrategyProperties strategyProperties;
    private final BacktestProperties backtestProperties;
    private final AtomicLong lastSignalEpochMs = new AtomicLong(0);

    public LiveSignalTrigger(
            MarketStateStore marketStateStore,
            StrategySignalEvaluator evaluator,
            StrategySignalPublisher publisher,
            StrategyProperties strategyProperties,
            BacktestProperties backtestProperties) {
        this.marketStateStore = marketStateStore;
        this.evaluator = evaluator;
        this.publisher = publisher;
        this.strategyProperties = strategyProperties;
        this.backtestProperties = backtestProperties;
    }

    @Scheduled(fixedDelayString = "${app.strategy.trigger-ms}")
    public void run() {
        if (backtestProperties.isEnabled()) {
            return;
        }
        Double mark = marketStateStore.getMarkPrice();
        List<com.trading.strategy.model.Candle> candles = marketStateStore.snapshotClosedCandles();
        if (mark == null || candles.isEmpty()) {
            return;
        }

        StrategyDecision decision = evaluator.evaluate(candles, mark);
        if (!decision.shouldSignal()) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        long previous = lastSignalEpochMs.get();
        if ((now - previous) < strategyProperties.getMinSignalIntervalMs()) {
            return;
        }
        if (!lastSignalEpochMs.compareAndSet(previous, now)) {
            return;
        }

        publisher.publishTestSignal(strategyProperties.getSymbol(), decision.side(), decision.signalPrice());
        log.info("Signal emitted side={} signalPrice(close)={} mark={} avgTop={} avgBottom={}",
                decision.side(), decision.signalPrice(), mark, decision.avgTop(), decision.avgBottom());
    }
}
