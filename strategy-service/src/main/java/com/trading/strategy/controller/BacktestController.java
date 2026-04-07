package com.trading.strategy.controller;

import com.trading.strategy.backtest.support.BacktestDateParser;
import com.trading.strategy.backtest.BacktestJobService;
import com.trading.strategy.persistence.BacktestJobRepository;
import com.trading.strategy.persistence.BacktestJobRow;
import com.trading.strategy.backtest.BacktestRequestSentinels;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/backtest")
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestController {

    private final BacktestJobService jobService;

    public BacktestController(BacktestJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/run")
    public ResponseEntity<BacktestRunResponse> run(
            @RequestBody(required = false) BacktestRunRequest request) {
        BacktestRunRequest req = request == null ? new BacktestRunRequest(null, null) : request;
        boolean noStart = req.startDate() == null || req.startDate().isBlank();
        boolean noEnd = req.endDate() == null || req.endDate().isBlank();
        if (noStart && !noEnd) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "startDate is required when endDate is provided");
        }
        if (!noStart) {
            try {
                BacktestDateParser.parse(req.startDate());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
        if (!noEnd) {
            try {
                BacktestDateParser.parse(req.endDate());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
        UUID jobId;
        if (noStart && noEnd) {
            jobId = jobService.createJob(BacktestRequestSentinels.LAST_1500_KLINES, null);
        } else {
            String endRaw = noEnd ? null : req.endDate();
            jobId = jobService.createJob(req.startDate(), endRaw);
        }
        jobService.startJobAsync(jobId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/backtest/jobs/" + jobId)
                .build()
                .toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .body(new BacktestRunResponse(
                        jobId.toString(), "/api/v1/backtest/jobs/" + jobId));
    }

    @GetMapping("/jobs/{jobId}")
    public BacktestJobStatusResponse get(@PathVariable("jobId") UUID jobId) {
        BacktestJobRow row = jobService
                .getJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown jobId"));
        return BacktestJobStatusResponse.from(row);
    }

    /** If both {@code startDate} and {@code endDate} are absent, the job uses the latest 1500 fully closed klines. */
    public record BacktestRunRequest(String startDate, String endDate) {}

    public record BacktestRunResponse(String jobId, String statusPath) {}

    public record BacktestJobStatusResponse(
            String jobId,
            String status,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Instant effectiveStartDate,
            Instant effectiveEndDate,
            String errorMessage,
            Result result) {

        static BacktestJobStatusResponse from(BacktestJobRow row) {
            Instant effStart = row.effectiveStartMs() != null
                    ? Instant.ofEpochMilli(row.effectiveStartMs())
                    : null;
            Instant effEnd = row.effectiveEndMs() != null ? Instant.ofEpochMilli(row.effectiveEndMs()) : null;
            Result res = null;
            if (BacktestJobRepository.STATUS_SUCCEEDED.equals(row.status()) && row.candlesReplayed() != null) {
                SimulateResult sim = null;
                if (row.simBalanceUsdt() != null) {
                    sim = new SimulateResult(
                            row.simBalanceUsdt(),
                            row.simLastMarkPrice(),
                            row.simFrozen(),
                            row.simTotalTrades(),
                            row.simWinCount(),
                            row.simLoseCount(),
                            row.simLiquidationCount(),
                            row.simTotalPnl(),
                            row.simTotalFees(),
                            row.simOpenPositionActive());
                }
                res = new Result(row.candlesReplayed(), sim);
            }
            return new BacktestJobStatusResponse(
                    row.id().toString(),
                    row.status(),
                    row.createdAt(),
                    row.startedAt(),
                    row.finishedAt(),
                    effStart,
                    effEnd,
                    row.errorMessage(),
                    res);
        }

        public record Result(int candlesReplayed, SimulateResult simulate) {}

        public record SimulateResult(
                double balanceUsdt,
                Double lastMarkPrice,
                Boolean frozen,
                Integer totalTrades,
                Integer winCount,
                Integer loseCount,
                Integer liquidationCount,
                Double totalPnl,
                Double totalFees,
                Boolean openPositionActive) {}
    }
}
