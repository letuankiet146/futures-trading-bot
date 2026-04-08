package com.trading.simulate.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "positions")
public class PaperPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32)
    private String symbol;
    @Column(nullable = false, length = 8)
    private String side;
    @Column(nullable = false)
    private double quantity;
    @Column(nullable = false)
    private double entryPrice;
    @Column(nullable = false)
    private double takeProfitPrice;
    @Column(nullable = false)
    private double stopLossPrice;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(length = 128)
    private String correlationId;
    @Column(nullable = false)
    private Instant openedAt;

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }
    public double getTakeProfitPrice() { return takeProfitPrice; }
    public void setTakeProfitPrice(double takeProfitPrice) { this.takeProfitPrice = takeProfitPrice; }
    public double getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(double stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
}
