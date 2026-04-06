package com.trading.execute.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BacktestTradingModeStartupValidator implements ApplicationRunner {

    private final String tradingMode;
    private final boolean backtestEnabled;

    public BacktestTradingModeStartupValidator(
            @Value("${trading.mode:SIMULATE}") String tradingMode,
            @Value("${app.backtest.enabled:false}") boolean backtestEnabled) {
        this.tradingMode = tradingMode;
        this.backtestEnabled = backtestEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!backtestEnabled) {
            return;
        }
        if ("LIVE".equalsIgnoreCase(tradingMode.trim())) {
            throw new IllegalStateException(
                    "Invalid configuration: BACKTEST_ENABLED=true requires TRADING_MODE=SIMULATE. "
                            + "Live execution must not consume historical replay signals; set TRADING_MODE=SIMULATE or disable backtest.");
        }
    }
}
