package com.trading.execute.persistence.repository;

import com.trading.execute.persistence.entity.LiveOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveOrderRepository extends JpaRepository<LiveOrderEntity, Long> {
}
