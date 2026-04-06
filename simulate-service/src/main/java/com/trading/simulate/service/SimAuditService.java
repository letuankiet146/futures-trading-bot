package com.trading.simulate.service;

import com.trading.simulate.persistence.entity.SimAuditLogEntity;
import com.trading.simulate.persistence.repository.SimAuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SimAuditService {
    private final SimAuditLogRepository repository;

    public SimAuditService(SimAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String type, String correlationId, String message) {
        SimAuditLogEntity e = new SimAuditLogEntity();
        e.setEventTime(Instant.now());
        e.setEventType(type);
        e.setCorrelationId(correlationId);
        e.setMessage(message);
        repository.save(e);
    }
}
