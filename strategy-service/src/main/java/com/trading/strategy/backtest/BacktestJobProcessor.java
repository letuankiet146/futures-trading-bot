package com.trading.strategy.backtest;

import com.trading.strategy.backtest.support.BacktestDateParser;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.market.BinanceKlineRestClient;
import com.trading.strategy.model.Candle;
import com.trading.strategy.persistence.BacktestJobRepository;
import com.trading.strategy.persistence.BacktestJobRow;
import com.trading.strategy.persistence.KlineCacheRepository;
import com.trading.strategy.persistence.SimulateBacktestSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(BacktestJobProcessor.class);

    private final BacktestJobRepository jobRepo;
    private final StrategyProperties strategyProperties;
    private final KlineBackfillService backfill;
    private final KlineCacheRepository klineCache;
    private final BacktestReplayService replayService;
    private final BinanceKlineRestClient binanceKlineRestClient;
    private final SimulateBacktestSnapshotClient simulateSnapshotClient;

    public BacktestJobProcessor(
            BacktestJobRepository jobRepo,
            StrategyProperties strategyProperties,
            KlineBackfillService backfill,
            KlineCacheRepository klineCache,
            BacktestReplayService replayService,
            BinanceKlineRestClient binanceKlineRestClient,
            SimulateBacktestSnapshotClient simulateSnapshotClient) {
        this.jobRepo = jobRepo;
        this.strategyProperties = strategyProperties;
        this.backfill = backfill;
        this.klineCache = klineCache;
        this.replayService = replayService;
        this.binanceKlineRestClient = binanceKlineRestClient;
        this.simulateSnapshotClient = simulateSnapshotClient;
    }

    @Async(com.trading.strategy.config.BacktestAsyncConfig.EXECUTOR)
    public void processAsync(UUID jobId) {
        process(jobId);
    }

    void process(UUID jobId) {
        Optional<BacktestJobRow> rowOpt = jobRepo.findById(jobId);
        if (rowOpt.isEmpty()) {
            log.warn("Backtest job {} not found", jobId);
            return;
        }
        BacktestJobRow row = rowOpt.get();
        if (!BacktestJobRepository.STATUS_PENDING.equals(row.status())) {
            log.debug("Backtest job {} status {} — skip", jobId, row.status());
            return;
        }
        int updated = jobRepo.markRunning(jobId);
        if (updated == 0) {
            return;
        }
        try {
            long nowMs = System.currentTimeMillis();
            String symbol = strategyProperties.getSymbol();
            String interval = strategyProperties.getInterval();
            if (BacktestRequestSentinels.isLast1500KlinesRequest(row.requestStartRaw())) {
                processLast1500Klines(jobId, symbol, interval, nowMs);
                return;
            }
            Instant startInst = BacktestDateParser.parse(row.requestStartRaw());
            long latestClosedOpen = backfill.resolveLatestClosedOpenTimeMs(symbol, interval, nowMs);
            long effEndOpen;
            if (row.requestEndRaw() == null || row.requestEndRaw().isBlank()) {
                effEndOpen = latestClosedOpen;
            } else {
                Instant userEnd = BacktestDateParser.parse(row.requestEndRaw());
                long userEndMs = userEnd.toEpochMilli();
                effEndOpen = Math.min(userEndMs, latestClosedOpen);
            }
            long effStartOpen = startInst.toEpochMilli();
            if (effStartOpen > effEndOpen) {
                jobRepo.markFailed(jobId, "startDate is after effective end (latest closed kline or clamped endDate)");
                return;
            }
            jobRepo.updateEffectiveRange(jobId, effStartOpen, effEndOpen);
            backfill.ensureRangeCached(symbol, interval, effStartOpen, effEndOpen);
            List<Candle> candles = klineCache.findCandlesInRange(symbol, interval, effStartOpen, effEndOpen);
            int signals = replayService.replay(candles, jobId.toString());
            SimulateBacktestSnapshot snapshot =
                    simulateSnapshotClient.fetchSnapshotAfterReplay(jobId.toString(), signals);
            jobRepo.markSucceeded(jobId, candles.size(), snapshot);
            log.info(
                    "Backtest job {} SUCCEEDED candles={} signals={}",
                    jobId,
                    candles.size(),
                    signals);
        } catch (Exception e) {
            log.error("Backtest job {} FAILED", jobId, e);
            jobRepo.markFailed(jobId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private void processLast1500Klines(UUID jobId, String symbol, String interval, long nowMs) {
        List<Candle> page = binanceKlineRestClient.fetchKlinesPage(
                symbol, interval, null, null, BacktestRequestSentinels.LAST_1500_COUNT);
        List<Candle> closed = new ArrayList<>();
        for (Candle c : page) {
            if (c.getCloseTime() <= nowMs) {
                closed.add(c);
            }
        }
        if (closed.isEmpty()) {
            jobRepo.markFailed(jobId, "No closed klines returned for last-1500 mode");
            return;
        }
        klineCache.insertIgnoreBatch(closed, symbol, interval);
        long effStart = closed.get(0).getOpenTime();
        long effEnd = closed.get(closed.size() - 1).getOpenTime();
        jobRepo.updateEffectiveRange(jobId, effStart, effEnd);
        int signals = replayService.replay(closed, jobId.toString());
        SimulateBacktestSnapshot snapshot =
                simulateSnapshotClient.fetchSnapshotAfterReplay(jobId.toString(), signals);
        jobRepo.markSucceeded(jobId, closed.size(), snapshot);
        log.info(
                "Backtest job {} SUCCEEDED (last-1500) candles={} signals={}",
                jobId,
                closed.size(),
                signals);
    }
}
