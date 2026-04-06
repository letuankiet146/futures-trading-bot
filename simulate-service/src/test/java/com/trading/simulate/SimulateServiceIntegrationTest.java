package com.trading.simulate;

import com.trading.simulate.persistence.repository.PaperAccountSnapshotRepository;
import com.trading.simulate.persistence.repository.PaperFillRepository;
import com.trading.simulate.persistence.repository.PaperOrderRepository;
import com.trading.simulate.persistence.repository.PaperPositionRepository;
import com.trading.simulate.persistence.repository.SimAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SimulateServiceIntegrationTest {
    @Autowired
    private PaperAccountSnapshotRepository snapshotRepository;
    @Autowired
    private PaperOrderRepository orderRepository;
    @Autowired
    private PaperPositionRepository positionRepository;
    @Autowired
    private PaperFillRepository fillRepository;
    @Autowired
    private SimAuditLogRepository auditRepository;

    @Test
    void contextAndRepositoriesLoad() {
        assertThat(snapshotRepository).isNotNull();
        assertThat(orderRepository).isNotNull();
        assertThat(positionRepository).isNotNull();
        assertThat(fillRepository).isNotNull();
        assertThat(auditRepository).isNotNull();
    }
}
