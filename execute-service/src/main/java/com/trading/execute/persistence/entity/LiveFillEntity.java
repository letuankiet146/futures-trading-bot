package com.trading.execute.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fills")
public class LiveFillEntity {
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
    private double price;
    @Column(nullable = false, length = 64)
    private String fillType;
    @Column(nullable = false)
    private Instant fillTime;

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getFillType() { return fillType; }
    public void setFillType(String fillType) { this.fillType = fillType; }
    public Instant getFillTime() { return fillTime; }
    public void setFillTime(Instant fillTime) { this.fillTime = fillTime; }
}
