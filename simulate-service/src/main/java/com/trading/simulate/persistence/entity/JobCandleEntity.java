package com.trading.simulate.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_candle")
public class JobCandleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 128)
    private String jobId;
    @Column(nullable = false)
    private Instant ts;
    @Column(nullable = false)
    private double open;
    @Column(nullable = false)
    private double high;
    @Column(nullable = false)
    private double low;
    @Column(nullable = false)
    private double close;
    @Column(nullable = false)
    private double volume;

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }
    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }
    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }
    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
}
