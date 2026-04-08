package com.trading.simulate;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.model.JobTimeline;
import com.trading.simulate.service.PaperTradingService;
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
    @Autowired
    private PaperTradingService paperTradingService;

    @Test
    void contextAndRepositoriesLoad() {
        assertThat(snapshotRepository).isNotNull();
        assertThat(orderRepository).isNotNull();
        assertThat(positionRepository).isNotNull();
        assertThat(fillRepository).isNotNull();
        assertThat(auditRepository).isNotNull();
        assertThat(paperTradingService).isNotNull();
    }

    @Test
    void timelineContainsEntryAndExitEventsForJob() {
        StrategySignalEvent entry = new StrategySignalEvent();
        entry.setSchemaVersion(1);
        entry.setSymbol("BTCUSDT");
        entry.setSide("BUY");
        entry.setPrice(100.0);
        entry.setCorrelationId("job-it-1");
        entry.setTimestamp("2026-04-08T10:00:00Z");

        paperTradingService.onStrategySignal(entry);
        paperTradingService.onReplayMarkPrice(101.0, "job-it-1", "2026-04-08T10:01:00Z");

        JobTimeline timeline = paperTradingService.timeline("job-it-1");
        assertThat(timeline.candles()).isNotEmpty();
        assertThat(timeline.events()).extracting("type").contains("ENTRY", "TP");
        assertThat(timeline.balance()).isNotEmpty();
    }
}
