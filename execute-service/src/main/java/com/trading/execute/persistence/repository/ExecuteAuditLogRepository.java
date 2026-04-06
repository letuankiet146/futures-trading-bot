package com.trading.execute.persistence.repository;

import com.trading.execute.persistence.entity.ExecuteAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecuteAuditLogRepository extends JpaRepository<ExecuteAuditLogEntity, Long> {
}
