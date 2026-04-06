package com.trading.execute.persistence.repository;

import com.trading.execute.persistence.entity.LiveFillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveFillRepository extends JpaRepository<LiveFillEntity, Long> {
}
