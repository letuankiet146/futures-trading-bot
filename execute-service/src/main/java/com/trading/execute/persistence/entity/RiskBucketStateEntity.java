package com.trading.execute.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "risk_bucket_state")
public class RiskBucketStateEntity {
    @Id
    private Long id;
    @Column(nullable = false)
    private Instant bucketStart;
    @Column(nullable = false)
    private double openingEquity;
    @Column(nullable = false)
    private double realizedLoss;
    @Column(nullable = false)
    private boolean paused;
    @Column(nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getBucketStart() { return bucketStart; }
    public void setBucketStart(Instant bucketStart) { this.bucketStart = bucketStart; }
    public double getOpeningEquity() { return openingEquity; }
    public void setOpeningEquity(double openingEquity) { this.openingEquity = openingEquity; }
    public double getRealizedLoss() { return realizedLoss; }
    public void setRealizedLoss(double realizedLoss) { this.realizedLoss = realizedLoss; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
