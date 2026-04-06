package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.SimAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimAuditLogRepository extends JpaRepository<SimAuditLogEntity, Long> {
}
