package com.trading.strategy.backtest;

import com.trading.strategy.persistence.SimulateBacktestSnapshot;
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
            SimulateStateDto state = restClient.get()
                    .uri("/api/v1/simulate/state")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SimulateStateDto.class);
            JobTimelineDto timeline = restClient.get()
                    .uri("/api/v1/simulate/jobs/{jobId}/timeline", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JobTimelineDto.class);
            if (state == null || timeline == null || timeline.summary == null) {
                return null;
            }
            return new SimulateBacktestSnapshot(
                    state.balanceUsdt,
                    state.lastMarkPrice,
                    state.frozen,
                    timeline.summary.totalTrades,
                    timeline.summary.wins,
                    timeline.summary.losses,
                    timeline.summary.liquidations,
                    timeline.summary.totalPnl,
                    timeline.summary.totalFees,
                    state.openPosition != null && state.openPosition.active);
        } catch (Exception e) {
            log.warn("Backtest simulate snapshot unavailable for jobId={}: {}", jobId, e.getMessage());
            return null;
        }
    }

    private static class SimulateStateDto {
        public Double balanceUsdt;
        public Double lastMarkPrice;
        public boolean frozen;
        public PositionDto openPosition;
    }

    private static class PositionDto {
        public boolean active;
    }

    private static class JobTimelineDto {
        public SummaryDto summary;
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
