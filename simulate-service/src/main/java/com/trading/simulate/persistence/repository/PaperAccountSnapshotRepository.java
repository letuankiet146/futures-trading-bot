package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.PaperAccountSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperAccountSnapshotRepository extends JpaRepository<PaperAccountSnapshotEntity, Long> {
}
