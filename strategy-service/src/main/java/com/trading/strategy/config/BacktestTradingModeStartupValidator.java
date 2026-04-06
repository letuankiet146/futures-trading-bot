package com.trading.strategy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BacktestTradingModeStartupValidator implements ApplicationRunner {

    static final String VIOLATION_MESSAGE =
            "Invalid configuration: BACKTEST_ENABLED=true requires TRADING_MODE=SIMULATE. "
                    + "Historical replay must run against paper execution only; set TRADING_MODE=SIMULATE or disable backtest.";

    private final BacktestProperties backtestProperties;
    private final String tradingMode;

    public BacktestTradingModeStartupValidator(
            BacktestProperties backtestProperties, @Value("${trading.mode:SIMULATE}") String tradingMode) {
        this.backtestProperties = backtestProperties;
        this.tradingMode = tradingMode;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!backtestProperties.isEnabled()) {
            return;
        }
        if (!"SIMULATE".equalsIgnoreCase(tradingMode.trim())) {
            throw new IllegalStateException(VIOLATION_MESSAGE);
        }
    }
}
