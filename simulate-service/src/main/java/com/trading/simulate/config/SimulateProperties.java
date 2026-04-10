package com.trading.simulate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.simulate")
public class SimulateProperties {
    private String symbol;
    private int leverage;
    private double takerFee;
    private double feeGateMultiplier;
    private double initialBalanceUsdt;
    private Sizing sizing = new Sizing();
    private Liquidation liquidation = new Liquidation();
    private Auth auth = new Auth();
    private Latency latency = new Latency();

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getLeverage() {
        return leverage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    public double getTakerFee() {
        return takerFee;
    }

    public void setTakerFee(double takerFee) {
        this.takerFee = takerFee;
    }

    public double getFeeGateMultiplier() {
        return feeGateMultiplier;
    }

    public void setFeeGateMultiplier(double feeGateMultiplier) {
        this.feeGateMultiplier = feeGateMultiplier;
    }

    public double getInitialBalanceUsdt() {
        return initialBalanceUsdt;
    }

    public void setInitialBalanceUsdt(double initialBalanceUsdt) {
        this.initialBalanceUsdt = initialBalanceUsdt;
    }

    public Sizing getSizing() {
        return sizing;
    }

    public void setSizing(Sizing sizing) {
        this.sizing = sizing;
    }

    public Liquidation getLiquidation() {
        return liquidation;
    }

    public void setLiquidation(Liquidation liquidation) {
        this.liquidation = liquidation;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Latency getLatency() {
        return latency;
    }

    public void setLatency(Latency latency) {
        this.latency = latency;
    }

    public static class Sizing {
        private String mode;
        private double accountPercent;
        private double maxAccountPercent;
        private double fixedNotionalUsdt;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public double getAccountPercent() {
            return accountPercent;
        }

        public void setAccountPercent(double accountPercent) {
            this.accountPercent = accountPercent;
        }

        public double getMaxAccountPercent() {
            return maxAccountPercent;
        }

        public void setMaxAccountPercent(double maxAccountPercent) {
            this.maxAccountPercent = maxAccountPercent;
        }

        public double getFixedNotionalUsdt() {
            return fixedNotionalUsdt;
        }

        public void setFixedNotionalUsdt(double fixedNotionalUsdt) {
            this.fixedNotionalUsdt = fixedNotionalUsdt;
        }
    }

    public static class Liquidation {
        private double isolatedMarginLossThreshold;

        public double getIsolatedMarginLossThreshold() {
            return isolatedMarginLossThreshold;
        }

        public void setIsolatedMarginLossThreshold(double isolatedMarginLossThreshold) {
            this.isolatedMarginLossThreshold = isolatedMarginLossThreshold;
        }
    }

    public static class Auth {
        private String adminToken;

        public String getAdminToken() {
            return adminToken;
        }

        public void setAdminToken(String adminToken) {
            this.adminToken = adminToken;
        }
    }

    public static class Latency {
        private long warnMs;
        private long blockMs;

        public long getWarnMs() {
            return warnMs;
        }

        public void setWarnMs(long warnMs) {
            this.warnMs = warnMs;
        }

        public long getBlockMs() {
            return blockMs;
        }

        public void setBlockMs(long blockMs) {
            this.blockMs = blockMs;
        }
    }
}
