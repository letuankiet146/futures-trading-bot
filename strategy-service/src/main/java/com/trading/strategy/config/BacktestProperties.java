package com.trading.strategy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.backtest")
public class BacktestProperties {
    private boolean enabled;
    private boolean standaloneExit;
    private String ohlcOrder;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStandaloneExit() {
        return standaloneExit;
    }

    public void setStandaloneExit(boolean standaloneExit) {
        this.standaloneExit = standaloneExit;
    }

    public String getOhlcOrder() {
        return ohlcOrder;
    }

    public void setOhlcOrder(String ohlcOrder) {
        this.ohlcOrder = ohlcOrder;
    }
}
