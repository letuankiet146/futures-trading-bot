package com.trading.strategy.backtest;

import com.trading.strategy.config.BacktestProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.market.BinanceKlineRestClient;
import com.trading.strategy.model.Candle;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Optional one-shot CLI backtest on startup. Default is off; use REST API instead (see {@code BacktestController}).
 */
@Component
@ConditionalOnProperty(
        name = {"app.backtest.enabled", "app.backtest.cli-on-startup"},
        havingValue = "true")
public class BacktestRunner implements CommandLineRunner {
    private final BinanceKlineRestClient klineRestClient;
    private final StrategyProperties strategyProperties;
    private final BacktestProperties backtestProperties;
    private final BacktestReplayService replayService;
    private final ConfigurableApplicationContext applicationContext;

    public BacktestRunner(
            BinanceKlineRestClient klineRestClient,
            StrategyProperties strategyProperties,
            BacktestProperties backtestProperties,
            BacktestReplayService replayService,
            ConfigurableApplicationContext applicationContext) {
        this.klineRestClient = klineRestClient;
        this.strategyProperties = strategyProperties;
        this.backtestProperties = backtestProperties;
        this.replayService = replayService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        List<Candle> all = klineRestClient.loadClosedKlines(
                strategyProperties.getSymbol(),
                strategyProperties.getInterval(),
                strategyProperties.getN());
        replayService.replay(all, UUID.randomUUID().toString());

        if (backtestProperties.isStandaloneExit()) {
            int code = org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0);
            System.exit(code);
        }
    }
}
