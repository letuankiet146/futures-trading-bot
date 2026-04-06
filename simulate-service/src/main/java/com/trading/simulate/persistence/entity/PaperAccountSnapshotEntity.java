package com.trading.simulate.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "paper_account_snapshot")
public class PaperAccountSnapshotEntity {
    @Id
    private Long id;
    @Column(nullable = false)
    private double balanceUsdt;
    @Column
    private Double lastMarkPrice;
    @Column(nullable = false)
    private boolean frozen;
    @Column(nullable = false)
    private int winCount;
    @Column(nullable = false)
    private int loseCount;
    @Column(nullable = false)
    private int liquidationCount;
    @Column(nullable = false)
    private int totalTrades;
    @Column(nullable = false)
    private double totalPnl;
    @Column(nullable = false)
    private double totalFees;
    @Column(nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getBalanceUsdt() { return balanceUsdt; }
    public void setBalanceUsdt(double balanceUsdt) { this.balanceUsdt = balanceUsdt; }
    public Double getLastMarkPrice() { return lastMarkPrice; }
    public void setLastMarkPrice(Double lastMarkPrice) { this.lastMarkPrice = lastMarkPrice; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public int getWinCount() { return winCount; }
    public void setWinCount(int winCount) { this.winCount = winCount; }
    public int getLoseCount() { return loseCount; }
    public void setLoseCount(int loseCount) { this.loseCount = loseCount; }
    public int getLiquidationCount() { return liquidationCount; }
    public void setLiquidationCount(int liquidationCount) { this.liquidationCount = liquidationCount; }
    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }
    public double getTotalPnl() { return totalPnl; }
    public void setTotalPnl(double totalPnl) { this.totalPnl = totalPnl; }
    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
