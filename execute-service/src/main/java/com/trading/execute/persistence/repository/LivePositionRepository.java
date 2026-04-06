package com.trading.execute.persistence.repository;

import com.trading.execute.persistence.entity.LivePositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivePositionRepository extends JpaRepository<LivePositionEntity, Long> {
}
