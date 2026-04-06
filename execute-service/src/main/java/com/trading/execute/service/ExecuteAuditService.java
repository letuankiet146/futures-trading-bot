package com.trading.execute.service;

import com.trading.execute.persistence.entity.ExecuteAuditLogEntity;
import com.trading.execute.persistence.repository.ExecuteAuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ExecuteAuditService {
    private final ExecuteAuditLogRepository repository;

    public ExecuteAuditService(ExecuteAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String eventType, String correlationId, String message) {
        ExecuteAuditLogEntity e = new ExecuteAuditLogEntity();
        e.setEventTime(Instant.now());
        e.setEventType(eventType);
        e.setCorrelationId(correlationId);
        e.setMessage(message);
        repository.save(e);
    }
}
