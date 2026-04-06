package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.PaperFillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperFillRepository extends JpaRepository<PaperFillEntity, Long> {
}
