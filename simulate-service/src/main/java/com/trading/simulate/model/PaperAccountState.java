package com.trading.simulate.model;

public class PaperAccountState {
    private double balanceUsdt;
    private Double lastMarkPrice;
    private boolean frozen;
    private PaperPosition openPosition;
    private PaperStats stats = new PaperStats();

    public double getBalanceUsdt() {
        return balanceUsdt;
    }

    public void setBalanceUsdt(double balanceUsdt) {
        this.balanceUsdt = balanceUsdt;
    }

    public Double getLastMarkPrice() {
        return lastMarkPrice;
    }

    public void setLastMarkPrice(Double lastMarkPrice) {
        this.lastMarkPrice = lastMarkPrice;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public PaperPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(PaperPosition openPosition) {
        this.openPosition = openPosition;
    }

    public PaperStats getStats() {
        return stats;
    }

    public void setStats(PaperStats stats) {
        this.stats = stats;
    }
}
