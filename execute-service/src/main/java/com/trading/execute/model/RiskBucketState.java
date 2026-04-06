package com.trading.execute.model;

import java.time.Instant;

public class RiskBucketState {
    private Instant bucketStart;
    private double openingEquity;
    private double realizedLoss;
    private boolean paused;

    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public double getOpeningEquity() {
        return openingEquity;
    }

    public void setOpeningEquity(double openingEquity) {
        this.openingEquity = openingEquity;
    }

    public double getRealizedLoss() {
        return realizedLoss;
    }

    public void setRealizedLoss(double realizedLoss) {
        this.realizedLoss = realizedLoss;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}
