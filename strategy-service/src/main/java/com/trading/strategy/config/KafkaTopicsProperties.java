package com.trading.strategy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsProperties {
    private String strategySignal;

    public String getStrategySignal() {
        return strategySignal;
    }

    public void setStrategySignal(String strategySignal) {
        this.strategySignal = strategySignal;
    }
}
