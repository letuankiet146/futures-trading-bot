package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.JobBalanceSnapshotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobBalanceSnapshotRepository extends JpaRepository<JobBalanceSnapshotEntity, Long> {
    List<JobBalanceSnapshotEntity> findByJobIdOrderByEventTimeAsc(String jobId);
}
