package com.trading.simulate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsProperties {
    private String strategySignal;
    private String simulateReplay;

    public String getStrategySignal() {
        return strategySignal;
    }

    public void setStrategySignal(String strategySignal) {
        this.strategySignal = strategySignal;
    }

    public String getSimulateReplay() {
        return simulateReplay;
    }

    public void setSimulateReplay(String simulateReplay) {
        this.simulateReplay = simulateReplay;
    }
}
