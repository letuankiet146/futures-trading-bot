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
    private final double initialBalanceUsdt;
    private final int snapshotPollMaxAttempts;
    private final long snapshotPollDelayMs;

    public SimulateBacktestSnapshotClient(
            @Value("${app.simulate.base-url:http://simulate-service:8083}") String baseUrl,
            @Value("${app.simulate.initial-balance-usdt:500}") double initialBalanceUsdt,
            @Value("${app.backtest.snapshot-poll-max-attempts:50}") int snapshotPollMaxAttempts,
            @Value("${app.backtest.snapshot-poll-delay-ms:200}") long snapshotPollDelayMs) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.initialBalanceUsdt = initialBalanceUsdt;
        this.snapshotPollMaxAttempts = Math.max(1, snapshotPollMaxAttempts);
        this.snapshotPollDelayMs = Math.max(50L, snapshotPollDelayMs);
    }

    /**
     * Poll simulate-service until replay Kafka messages are reflected in the job timeline, then persistable snapshot.
     */
    public SimulateBacktestSnapshot fetchSnapshotAfterReplay(String jobId, int expectedSignals) {
        SimulateBacktestSnapshot last = null;
        for (int attempt = 1; attempt <= snapshotPollMaxAttempts; attempt++) {
            last = fetchSnapshotOrNull(jobId);
            if (last == null) {
                sleep(snapshotPollDelayMs);
                continue;
            }
            if (expectedSignals == 0 || last.balanceUsdt() != null) {
                return last;
            }
            sleep(snapshotPollDelayMs);
        }
        if (last != null && last.balanceUsdt() == null && expectedSignals > 0) {
            log.warn(
                    "Backtest simulate snapshot for jobId={} missing balance after {} polls (expectedSignals={})",
                    jobId,
                    snapshotPollMaxAttempts,
                    expectedSignals);
        }
        return last;
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
            Double finalBalance = resolveFinalBalance(timeline);
            SimulateBacktestSnapshot.OpenPosition openPosition = null;
            if (timeline.openPosition != null) {
                OpenPositionDto p = timeline.openPosition;
                openPosition = new SimulateBacktestSnapshot.OpenPosition(
                        p.side,
                        p.entryPrice,
                        p.quantity,
                        p.takeProfitPrice,
                        p.stopLossPrice,
                        p.markPrice,
                        p.unrealizedPnl,
                        p.entryTime);
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
                    timeline.summary.openPositionActive,
                    timeline.summary.unrealizedPnl,
                    openPosition);
        } catch (Exception e) {
            log.warn("Backtest simulate snapshot unavailable for jobId={}: {}", jobId, e.getMessage());
            return null;
        }
    }

    private Double resolveFinalBalance(JobTimelineDto timeline) {
        if (timeline.balance != null && !timeline.balance.isEmpty()) {
            return timeline.balance.get(timeline.balance.size() - 1).balanceUsdt;
        }
        if (timeline.summary != null && timeline.summary.totalTrades == 0) {
            return initialBalanceUsdt;
        }
        return null;
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class JobTimelineDto {
        public SummaryDto summary;
        public List<BalancePointDto> balance;
        public OpenPositionDto openPosition;
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
        public double unrealizedPnl;
        public boolean openPositionActive;
    }

    private static class OpenPositionDto {
        public String side;
        public Double entryPrice;
        public Double quantity;
        public Double takeProfitPrice;
        public Double stopLossPrice;
        public Double markPrice;
        public Double unrealizedPnl;
        public java.time.Instant entryTime;
    }
}
