package com.trading.simulate.persistence.repository;

import com.trading.simulate.persistence.entity.JobTradeEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTradeEventRepository extends JpaRepository<JobTradeEventEntity, Long> {
    List<JobTradeEventEntity> findByJobIdOrderByEventTimeAsc(String jobId);
}
