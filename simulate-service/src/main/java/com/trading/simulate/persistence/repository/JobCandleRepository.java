package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.JobCandleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCandleRepository extends JpaRepository<JobCandleEntity, Long> {
    List<JobCandleEntity> findByJobIdOrderByTsAsc(String jobId);
}
