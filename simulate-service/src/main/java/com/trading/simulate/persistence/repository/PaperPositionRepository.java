package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.PaperPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperPositionRepository extends JpaRepository<PaperPositionEntity, Long> {
}
