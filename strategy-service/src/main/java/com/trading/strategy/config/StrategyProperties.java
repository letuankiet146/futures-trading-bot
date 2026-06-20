package com.trading.strategy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.strategy")
public class StrategyProperties {
    private String type = "peak-trough";
    private String symbol;
    private String interval;
    private int n;
    private int k;
    private double takerFee;
    private double feeGateMultiplier;
    private double similarityThreshold;
    private long triggerMs;
    private long minSignalIntervalMs;
    private Exit exit = new Exit();
    private VortexScalping vortex = new VortexScalping();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public VortexScalping getVortex() {
        return vortex;
    }

    public void setVortex(VortexScalping vortex) {
        this.vortex = vortex;
    }

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

    public double getFeeGateMultiplier() {
        return feeGateMultiplier;
    }

    public void setFeeGateMultiplier(double feeGateMultiplier) {
        this.feeGateMultiplier = feeGateMultiplier;
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

    public Exit getExit() {
        return exit;
    }

    public void setExit(Exit exit) {
        this.exit = exit;
    }

    public static class Exit {
        private double takeProfitPercent;
        private double stopLossPercent;
        private double tpMultiplier = 1.0;
        private double slMultiplier = 1.0;

        public double getTakeProfitPercent() {
            return takeProfitPercent;
        }

        public void setTakeProfitPercent(double takeProfitPercent) {
            this.takeProfitPercent = takeProfitPercent;
        }

        public double getStopLossPercent() {
            return stopLossPercent;
        }

        public void setStopLossPercent(double stopLossPercent) {
            this.stopLossPercent = stopLossPercent;
        }

        public double getTpMultiplier() {
            return tpMultiplier;
        }

        public void setTpMultiplier(double tpMultiplier) {
            this.tpMultiplier = tpMultiplier;
        }

        public double getSlMultiplier() {
            return slMultiplier;
        }

        public void setSlMultiplier(double slMultiplier) {
            this.slMultiplier = slMultiplier;
        }
    }

    public static class VortexScalping {
        private int emaPeriod = 20;
        private int rsiPeriod = 14;
        private double rsiOversold = 40.0;
        private double rsiOverbought = 60.0;
        private int volumeLookback = 5;
        private double takerBuyVolumeMultiplier = 1.5;
        private double takerBuyRatioDropFactor = 0.7;

        public int getEmaPeriod() {
            return emaPeriod;
        }

        public void setEmaPeriod(int emaPeriod) {
            this.emaPeriod = emaPeriod;
        }

        public int getRsiPeriod() {
            return rsiPeriod;
        }

        public void setRsiPeriod(int rsiPeriod) {
            this.rsiPeriod = rsiPeriod;
        }

        public double getRsiOversold() {
            return rsiOversold;
        }

        public void setRsiOversold(double rsiOversold) {
            this.rsiOversold = rsiOversold;
        }

        public double getRsiOverbought() {
            return rsiOverbought;
        }

        public void setRsiOverbought(double rsiOverbought) {
            this.rsiOverbought = rsiOverbought;
        }

        public int getVolumeLookback() {
            return volumeLookback;
        }

        public void setVolumeLookback(int volumeLookback) {
            this.volumeLookback = volumeLookback;
        }

        public double getTakerBuyVolumeMultiplier() {
            return takerBuyVolumeMultiplier;
        }

        public void setTakerBuyVolumeMultiplier(double takerBuyVolumeMultiplier) {
            this.takerBuyVolumeMultiplier = takerBuyVolumeMultiplier;
        }

        public double getTakerBuyRatioDropFactor() {
            return takerBuyRatioDropFactor;
        }

        public void setTakerBuyRatioDropFactor(double takerBuyRatioDropFactor) {
            this.takerBuyRatioDropFactor = takerBuyRatioDropFactor;
        }
    }
}
