package com.trading.strategy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.strategy")
public class StrategyProperties {
    private String symbol;
    private String interval;
    private int n;
    private int k;
    private double takerFee;
    private double similarityThreshold;
    private long triggerMs;
    private long minSignalIntervalMs;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public double getTakerFee() {
        return takerFee;
    }

    public void setTakerFee(double takerFee) {
        this.takerFee = takerFee;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public long getTriggerMs() {
        return triggerMs;
    }

    public void setTriggerMs(long triggerMs) {
        this.triggerMs = triggerMs;
    }

    public long getMinSignalIntervalMs() {
        return minSignalIntervalMs;
    }

    public void setMinSignalIntervalMs(long minSignalIntervalMs) {
        this.minSignalIntervalMs = minSignalIntervalMs;
    }
}
