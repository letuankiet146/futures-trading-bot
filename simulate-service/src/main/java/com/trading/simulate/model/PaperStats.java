package com.trading.simulate.model;

public class PaperStats {
    private int winCount;
    private int loseCount;
    private int liquidationCount;
    private int totalTrades;
    private double totalPnl;
    private double totalFees;

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(int loseCount) {
        this.loseCount = loseCount;
    }

    public int getLiquidationCount() {
        return liquidationCount;
    }

    public void setLiquidationCount(int liquidationCount) {
        this.liquidationCount = liquidationCount;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(int totalTrades) {
        this.totalTrades = totalTrades;
    }

    public double getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(double totalPnl) {
        this.totalPnl = totalPnl;
    }

    public double getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(double totalFees) {
        this.totalFees = totalFees;
    }
}
