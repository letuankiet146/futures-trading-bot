package com.trading.simulate.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_balance_snapshot")
public class JobBalanceSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 128)
    private String jobId;
    @Column(nullable = false)
    private double balanceUsdt;
    @Column(nullable = false)
    private Instant eventTime;

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public double getBalanceUsdt() { return balanceUsdt; }
    public void setBalanceUsdt(double balanceUsdt) { this.balanceUsdt = balanceUsdt; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
}
