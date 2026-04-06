package com.trading.execute.persistence.repository;

import com.trading.execute.persistence.entity.RiskBucketStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskBucketStateRepository extends JpaRepository<RiskBucketStateEntity, Long> {
}
