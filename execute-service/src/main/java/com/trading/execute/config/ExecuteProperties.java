package com.trading.execute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.execute")
public class ExecuteProperties {
    private String symbol;
    private String interval;
    private String mode;
    private int leverage;
    private double takerFee;
    private Sizing sizing = new Sizing();
    private Risk risk = new Risk();
    private Auth auth = new Auth();
    private Latency latency = new Latency();
    private Reconciliation reconciliation = new Reconciliation();

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public Sizing getSizing() {
        return sizing;
    }

    public void setSizing(Sizing sizing) {
        this.sizing = sizing;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
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

    public Reconciliation getReconciliation() {
        return reconciliation;
    }

    public void setReconciliation(Reconciliation reconciliation) {
        this.reconciliation = reconciliation;
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

    public static class Risk {
        private double maxLossPercent;
        private int bucketHours;

        public double getMaxLossPercent() {
            return maxLossPercent;
        }

        public void setMaxLossPercent(double maxLossPercent) {
            this.maxLossPercent = maxLossPercent;
        }

        public int getBucketHours() {
            return bucketHours;
        }

        public void setBucketHours(int bucketHours) {
            this.bucketHours = bucketHours;
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

    public static class Reconciliation {
        private long syncMs;
        private double driftUsdt;

        public long getSyncMs() {
            return syncMs;
        }

        public void setSyncMs(long syncMs) {
            this.syncMs = syncMs;
        }

        public double getDriftUsdt() {
            return driftUsdt;
        }

        public void setDriftUsdt(double driftUsdt) {
            this.driftUsdt = driftUsdt;
        }
    }
}
