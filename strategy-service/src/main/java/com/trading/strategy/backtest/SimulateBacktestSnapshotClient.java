package com.trading.strategy.backtest;

import com.trading.strategy.persistence.SimulateBacktestSnapshot;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SimulateBacktestSnapshotClient {
    private static final Logger log = LoggerFactory.getLogger(SimulateBacktestSnapshotClient.class);

    private final RestClient restClient;

    public SimulateBacktestSnapshotClient(@Value("${app.simulate.base-url:http://simulate-service:8083}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public SimulateBacktestSnapshot fetchSnapshotOrNull(String jobId) {
        try {
            JobTimelineDto timeline = restClient.get()
                    .uri("/api/v1/simulate/jobs/{jobId}/timeline", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JobTimelineDto.class);
            if (timeline == null || timeline.summary == null) {
                return null;
            }
            Double finalBalance = null;
            if (timeline.balance != null && !timeline.balance.isEmpty()) {
                finalBalance = timeline.balance.get(timeline.balance.size() - 1).balanceUsdt;
            }
            return new SimulateBacktestSnapshot(
                    finalBalance,
                    null,
                    null,
                    timeline.summary.totalTrades,
                    timeline.summary.wins,
                    timeline.summary.losses,
                    timeline.summary.liquidations,
                    timeline.summary.totalPnl,
                    timeline.summary.totalFees,
                    null);
        } catch (Exception e) {
            log.warn("Backtest simulate snapshot unavailable for jobId={}: {}", jobId, e.getMessage());
            return null;
        }
    }

    private static class JobTimelineDto {
        public SummaryDto summary;
        public List<BalancePointDto> balance;
    }

    private static class BalancePointDto {
        public Double balanceUsdt;
    }

    private static class SummaryDto {
        public int wins;
        public int losses;
        public int liquidations;
        public int totalTrades;
        public double totalPnl;
        public double totalFees;
    }
}
