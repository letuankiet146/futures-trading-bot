package com.trading.simulate.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.simulate")
public class SimulateProperties {
    private String symbol;
    private int leverage;
    private double takerFee;
    private double initialBalanceUsdt;
    private Sizing sizing = new Sizing();
    private Liquidation liquidation = new Liquidation();
    private Averaging averaging = new Averaging();
    private Auth auth = new Auth();
    private Latency latency = new Latency();
    private Backtest backtest = new Backtest();

    public Backtest getBacktest() {
        return backtest;
    }

    public void setBacktest(Backtest backtest) {
        this.backtest = backtest;
    }

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

    public Averaging getAveraging() {
        return averaging;
    }

    public void setAveraging(Averaging averaging) {
        this.averaging = averaging;
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
        /** ISOLATED: only the position margin is collateral. CROSS: the whole wallet balance is collateral. */
        private String marginMode = "ISOLATED";
        private double isolatedMarginLossThreshold;

        public String getMarginMode() {
            return marginMode;
        }

        public void setMarginMode(String marginMode) {
            this.marginMode = marginMode;
        }

        public boolean isCross() {
            return "CROSS".equalsIgnoreCase(marginMode);
        }

        public double getIsolatedMarginLossThreshold() {
            return isolatedMarginLossThreshold;
        }

        public void setIsolatedMarginLossThreshold(double isolatedMarginLossThreshold) {
            this.isolatedMarginLossThreshold = isolatedMarginLossThreshold;
        }
    }

    public static class Averaging {
        /** When true, a same-side signal on an open position adds to it (DCA) instead of being ignored. */
        private boolean enabled = false;
        /** Max number of entry legs per position (initial + averages). 0 means unlimited. */
        private int maxEntries = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
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

    /** Intra-candle TP/SL drill-down settings for backtest replay. */
    public static class Backtest {
        /** Base URL of strategy-service used to fetch+cache finer-interval klines. */
        private String strategyBaseUrl = "http://strategy-service:8081";

        /** Interval ladder from coarsest to finest; drilling starts at the largest entry below the parent. */
        private List<String> intervalLadder = List.of("1h", "30m", "15m", "3m", "1m");

        /** When even this interval still straddles TP and SL, apply the tie-break. */
        private String finestInterval = "1m";

        /** Tie-break at the finest interval: which side is assumed to be hit first (TP or SL). */
        private String tieBreak = "SL";

        public String getStrategyBaseUrl() {
            return strategyBaseUrl;
        }

        public void setStrategyBaseUrl(String strategyBaseUrl) {
            this.strategyBaseUrl = strategyBaseUrl;
        }

        public List<String> getIntervalLadder() {
            return intervalLadder;
        }

        public void setIntervalLadder(List<String> intervalLadder) {
            this.intervalLadder = intervalLadder;
        }

        public String getFinestInterval() {
            return finestInterval;
        }

        public void setFinestInterval(String finestInterval) {
            this.finestInterval = finestInterval;
        }

        public String getTieBreak() {
            return tieBreak;
        }

        public void setTieBreak(String tieBreak) {
            this.tieBreak = tieBreak;
        }
    }
}
