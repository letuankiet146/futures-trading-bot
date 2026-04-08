package com.trading.simulate.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_trade_event")
public class JobTradeEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 128)
    private String jobId;
    @Column(nullable = false, length = 32)
    private String eventType;
    @Column(nullable = false, length = 8)
    private String side;
    @Column(nullable = false)
    private double price;
    @Column(nullable = false)
    private double quantity;
    @Column
    private Double tp;
    @Column
    private Double sl;
    @Column(nullable = false)
    private Instant eventTime;
    @Column(length = 2000)
    private String metadataJson;

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public Double getTp() { return tp; }
    public void setTp(Double tp) { this.tp = tp; }
    public Double getSl() { return sl; }
    public void setSl(Double sl) { this.sl = sl; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
