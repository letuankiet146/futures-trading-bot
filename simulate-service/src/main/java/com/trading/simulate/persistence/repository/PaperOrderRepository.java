package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.PaperOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperOrderRepository extends JpaRepository<PaperOrderEntity, Long> {
}
