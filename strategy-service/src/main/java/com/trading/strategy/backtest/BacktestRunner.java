package com.trading.strategy.backtest;

import com.trading.strategy.config.BacktestProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.engine.StrategySignalEvaluator;
import com.trading.strategy.kafka.BacktestSimulateFeedPublisher;
import com.trading.strategy.market.BinanceKlineRestClient;
import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BacktestRunner.class);

    private final BinanceKlineRestClient klineRestClient;
    private final StrategyProperties strategyProperties;
    private final BacktestProperties backtestProperties;
    private final StrategySignalEvaluator evaluator;
    private final BacktestSimulateFeedPublisher simulateFeedPublisher;
    private final ConfigurableApplicationContext applicationContext;

    public BacktestRunner(
            BinanceKlineRestClient klineRestClient,
            StrategyProperties strategyProperties,
            BacktestProperties backtestProperties,
            StrategySignalEvaluator evaluator,
            BacktestSimulateFeedPublisher simulateFeedPublisher,
            ConfigurableApplicationContext applicationContext) {
        this.klineRestClient = klineRestClient;
        this.strategyProperties = strategyProperties;
        this.backtestProperties = backtestProperties;
        this.evaluator = evaluator;
        this.simulateFeedPublisher = simulateFeedPublisher;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        List<Candle> all = klineRestClient.loadClosedKlines(
                strategyProperties.getSymbol(),
                strategyProperties.getInterval(),
                strategyProperties.getN());
        if (all.size() < (2 * strategyProperties.getK() + 2)) {
            log.warn("Not enough candles for backtest run.");
            return;
        }

        int emitted = 0;
        List<Candle> history = new ArrayList<>();
        for (Candle candle : all) {
            history.add(candle);
            if (history.size() < (2 * strategyProperties.getK() + 1)) {
                continue;
            }

            for (double mark : toPricePath(candle)) {
                StrategyDecision decision = evaluator.evaluate(history, mark);
                // Draft-idea: each OHLC step triggers strategy, then sends price to simulation channel (ordered MARK).
                simulateFeedPublisher.publishMark(strategyProperties.getSymbol(), mark);
                if (decision.shouldSignal()) {
                    simulateFeedPublisher.publishSignal(strategyProperties.getSymbol(), decision.side(), mark);
                    emitted++;
                }
                simulateFeedPublisher.flush();
            }
        }

        log.info("Backtest completed candles={} emittedSignals={} symbol={} interval={}",
                all.size(), emitted, strategyProperties.getSymbol(), strategyProperties.getInterval());

        if (backtestProperties.isStandaloneExit()) {
            int code = org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0);
            System.exit(code);
        }
    }

    private List<Double> toPricePath(Candle candle) {
        // OHLC default required by spec, with support for alternate OLHC mode.
        String order = backtestProperties.getOhlcOrder() == null ? "OHLC" : backtestProperties.getOhlcOrder().toUpperCase();
        if ("OLHC".equals(order)) {
            return List.of(candle.getOpen(), candle.getLow(), candle.getHigh(), candle.getClose());
        }
        return List.of(candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose());
    }
}
