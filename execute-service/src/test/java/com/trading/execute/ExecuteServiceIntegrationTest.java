package com.trading.execute;

import com.trading.execute.persistence.repository.ExecuteAuditLogRepository;
import com.trading.execute.persistence.repository.LiveFillRepository;
import com.trading.execute.persistence.repository.LiveOrderRepository;
import com.trading.execute.persistence.repository.LivePositionRepository;
import com.trading.execute.persistence.repository.RiskBucketStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExecuteServiceIntegrationTest {
    @Autowired
    private LiveOrderRepository orderRepository;
    @Autowired
    private LivePositionRepository positionRepository;
    @Autowired
    private LiveFillRepository fillRepository;
    @Autowired
    private RiskBucketStateRepository riskBucketStateRepository;
    @Autowired
    private ExecuteAuditLogRepository auditLogRepository;

    @Test
    void contextAndRepositoriesLoad() {
        assertThat(orderRepository).isNotNull();
        assertThat(positionRepository).isNotNull();
        assertThat(fillRepository).isNotNull();
        assertThat(riskBucketStateRepository).isNotNull();
        assertThat(auditLogRepository).isNotNull();
    }
}
