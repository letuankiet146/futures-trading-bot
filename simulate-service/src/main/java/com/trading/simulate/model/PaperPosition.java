package com.trading.simulate.model;

import java.time.Instant;

public class PaperPosition {
    private String jobId;
    private String side;
    private String symbol;
    private double entryPrice;
    private double quantity;
    private double notional;
    private double isolatedMargin;
    /** null means no take-profit on this position. */
    private Double takeProfitPrice;
    /** null means no stop-loss on this position. */
    private Double stopLossPrice;
    private double openFee;
    private boolean active;
    private Instant openedAt;
    private int entryCount = 1;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getNotional() {
        return notional;
    }

    public void setNotional(double notional) {
        this.notional = notional;
    }

    public double getIsolatedMargin() {
        return isolatedMargin;
    }

    public void setIsolatedMargin(double isolatedMargin) {
        this.isolatedMargin = isolatedMargin;
    }

    public Double getTakeProfitPrice() {
        return takeProfitPrice;
    }

    public void setTakeProfitPrice(Double takeProfitPrice) {
        this.takeProfitPrice = takeProfitPrice;
    }

    public Double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(Double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getOpenFee() {
        return openFee;
    }

    public void setOpenFee(double openFee) {
        this.openFee = openFee;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }
}
