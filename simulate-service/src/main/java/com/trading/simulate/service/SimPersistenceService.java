package com.trading.simulate.service;

import com.trading.simulate.config.SimulateProperties;
import com.trading.simulate.model.PaperAccountState;
import com.trading.simulate.model.JobTimeline;
import com.trading.simulate.model.JobTimelineBalance;
import com.trading.simulate.model.JobTimelineCandle;
import com.trading.simulate.model.JobTimelineEvent;
import com.trading.simulate.model.JobTimelineSummary;
import com.trading.simulate.model.PaperPosition;
import com.trading.simulate.model.PaperStats;
import com.trading.simulate.persistence.entity.JobBalanceSnapshotEntity;
import com.trading.simulate.persistence.entity.JobCandleEntity;
import com.trading.simulate.persistence.entity.JobTradeEventEntity;
import com.trading.simulate.persistence.entity.PaperAccountSnapshotEntity;
import com.trading.simulate.persistence.entity.PaperFillEntity;
import com.trading.simulate.persistence.entity.PaperOrderEntity;
import com.trading.simulate.persistence.entity.PaperPositionEntity;
import com.trading.simulate.persistence.repository.JobBalanceSnapshotRepository;
import com.trading.simulate.persistence.repository.JobCandleRepository;
import com.trading.simulate.persistence.repository.JobTradeEventRepository;
import com.trading.simulate.persistence.repository.PaperAccountSnapshotRepository;
import com.trading.simulate.persistence.repository.PaperFillRepository;
import com.trading.simulate.persistence.repository.PaperOrderRepository;
import com.trading.simulate.persistence.repository.PaperPositionRepository;
import com.trading.simulate.persistence.strategy.StrategyBacktestChartDataRepository;
import com.trading.simulate.persistence.strategy.StrategyBacktestChartDataRepository.BacktestJobKlineRange;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SimPersistenceService {
    private final PaperAccountSnapshotRepository snapshotRepository;
    private final PaperOrderRepository orderRepository;
    private final PaperPositionRepository positionRepository;
    private final PaperFillRepository fillRepository;
    private final JobCandleRepository jobCandleRepository;
    private final JobTradeEventRepository jobTradeEventRepository;
    private final JobBalanceSnapshotRepository jobBalanceSnapshotRepository;
    private final StrategyBacktestChartDataRepository strategyBacktestChartDataRepository;
    private final SimulateProperties simulateProperties;

    public SimPersistenceService(
            PaperAccountSnapshotRepository snapshotRepository,
            PaperOrderRepository orderRepository,
            PaperPositionRepository positionRepository,
            PaperFillRepository fillRepository,
            JobCandleRepository jobCandleRepository,
            JobTradeEventRepository jobTradeEventRepository,
            JobBalanceSnapshotRepository jobBalanceSnapshotRepository,
            StrategyBacktestChartDataRepository strategyBacktestChartDataRepository,
            SimulateProperties simulateProperties) {
        this.snapshotRepository = snapshotRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
        this.jobCandleRepository = jobCandleRepository;
        this.jobTradeEventRepository = jobTradeEventRepository;
        this.jobBalanceSnapshotRepository = jobBalanceSnapshotRepository;
        this.strategyBacktestChartDataRepository = strategyBacktestChartDataRepository;
        this.simulateProperties = simulateProperties;
    }

    public void saveSnapshot(PaperAccountState state) {
        PaperAccountSnapshotEntity e = new PaperAccountSnapshotEntity();
        e.setId(1L);
        e.setBalanceUsdt(state.getBalanceUsdt());
        e.setLastMarkPrice(state.getLastMarkPrice());
        e.setFrozen(state.isFrozen());
        PaperStats s = state.getStats();
        e.setWinCount(s.getWinCount());
        e.setLoseCount(s.getLoseCount());
        e.setLiquidationCount(s.getLiquidationCount());
        e.setTotalTrades(s.getTotalTrades());
        e.setTotalPnl(s.getTotalPnl());
        e.setTotalFees(s.getTotalFees());
        e.setUpdatedAt(Instant.now());
        snapshotRepository.save(e);
    }

    public void saveOrder(String symbol, String side, double qty, double price, String status, String correlationId) {
        PaperOrderEntity o = new PaperOrderEntity();
        o.setSymbol(symbol);
        o.setSide(side);
        o.setQuantity(qty);
        o.setPrice(price);
        o.setStatus(status);
        o.setCorrelationId(correlationId);
        o.setCreatedAt(Instant.now());
        orderRepository.save(o);
    }

    public void savePosition(PaperPosition p, String status, String correlationId) {
        PaperPositionEntity e = new PaperPositionEntity();
        e.setSymbol(p.getSymbol());
        e.setSide(p.getSide());
        e.setQuantity(p.getQuantity());
        e.setEntryPrice(p.getEntryPrice());
        e.setTakeProfitPrice(p.getTakeProfitPrice());
        e.setStopLossPrice(p.getStopLossPrice());
        e.setStatus(status);
        e.setOpenedAt(p.getOpenedAt() == null ? Instant.now() : p.getOpenedAt());
        e.setCorrelationId(correlationId);
        positionRepository.save(e);
    }

    public void saveFill(String symbol, String side, double qty, double price, String outcome) {
        PaperFillEntity f = new PaperFillEntity();
        f.setSymbol(symbol);
        f.setSide(side);
        f.setQuantity(qty);
        f.setPrice(price);
        f.setOutcome(outcome);
        f.setFillTime(Instant.now());
        fillRepository.save(f);
    }

    public void saveJobCandle(String jobId, Instant ts, double price) {
        JobCandleEntity e = new JobCandleEntity();
        e.setJobId(jobId);
        e.setTs(ts);
        e.setOpen(price);
        e.setHigh(price);
        e.setLow(price);
        e.setClose(price);
        e.setVolume(0.0);
        jobCandleRepository.save(e);
    }

    public void saveTradeEvent(String jobId, String eventType, String side, double price, double quantity, Double tp, Double sl, Instant eventTime) {
        JobTradeEventEntity e = new JobTradeEventEntity();
        e.setJobId(jobId);
        e.setEventType(eventType);
        e.setSide(side);
        e.setPrice(price);
        e.setQuantity(quantity);
        e.setTp(tp);
        e.setSl(sl);
        e.setEventTime(eventTime);
        jobTradeEventRepository.save(e);
    }

    public void saveJobBalance(String jobId, double balanceUsdt, Instant eventTime) {
        JobBalanceSnapshotEntity e = new JobBalanceSnapshotEntity();
        e.setJobId(jobId);
        e.setBalanceUsdt(balanceUsdt);
        e.setEventTime(eventTime);
        jobBalanceSnapshotRepository.save(e);
    }

    public JobTimeline loadTimeline(String jobId) {
        List<JobTimelineCandle> chartCandles = loadChartCandlesFromStrategyOrFallback(jobId);
        List<JobTimelineEvent> events = jobTradeEventRepository.findByJobIdOrderByEventTimeAsc(jobId).stream()
                .map(e -> new JobTimelineEvent(e.getEventType(), e.getSide(), e.getPrice(), e.getQuantity(), e.getTp(), e.getSl(), e.getEventTime()))
                .toList();
        List<JobTimelineBalance> balance = jobBalanceSnapshotRepository.findByJobIdOrderByEventTimeAsc(jobId).stream()
                .map(b -> new JobTimelineBalance(b.getEventTime(), b.getBalanceUsdt()))
                .toList();
        double takerFeeRate = simulateProperties.getTakerFee();
        int wins = 0;
        int losses = 0;
        int liquidations = 0;
        // Pair each ENTRY with its following exit so reverse-on-signal closes are also counted. A REVERSE
        // close is classified by net PnL (after open+close fees), matching the in-memory PaperStats accounting.
        JobTimelineEvent openEntry = null;
        for (JobTimelineEvent e : events) {
            String type = e.type();
            if ("ENTRY".equals(type)) {
                openEntry = e;
                continue;
            }
            switch (type) {
                case "TP" -> wins++;
                case "SL" -> losses++;
                case "LIQUIDATED" -> liquidations++;
                case "REVERSE" -> {
                    if (isReverseWin(openEntry, e, takerFeeRate)) {
                        wins++;
                    } else {
                        losses++;
                    }
                }
                default -> {
                    continue;
                }
            }
            openEntry = null;
        }
        int totalTrades = wins + losses + liquidations;
        // Liquidations charge no close fee (see PaperTradingService#liquidate), so they are excluded here.
        double totalFees = events.stream()
                .filter(e -> "ENTRY".equals(e.type())
                        || "TP".equals(e.type())
                        || "SL".equals(e.type())
                        || "REVERSE".equals(e.type()))
                .mapToDouble(e -> e.price() * e.quantity() * takerFeeRate)
                .sum();
        double totalPnl = 0.0;
        if (balance.size() >= 2) {
            totalPnl = balance.get(balance.size() - 1).balanceUsdt() - balance.get(0).balanceUsdt();
        }
        // A position still open at the end has no realized balance change yet; mark it to market using the
        // last candle close so its unrealized PnL is reflected in totalPnl.
        if (openEntry != null && !chartCandles.isEmpty()) {
            double lastClose = chartCandles.get(chartCandles.size() - 1).close();
            double qty = openEntry.quantity();
            double unrealizedPnl = "BUY".equalsIgnoreCase(openEntry.side())
                    ? (lastClose - openEntry.price()) * qty
                    : (openEntry.price() - lastClose) * qty;
            totalPnl += unrealizedPnl;
        }
        JobTimelineSummary summary = new JobTimelineSummary(
                wins,
                losses,
                liquidations,
                totalTrades,
                totalPnl,
                totalFees);
        return new JobTimeline(jobId, chartCandles, events, balance, summary);
    }

    /**
     * Classifies a reverse-on-signal close as win/lose by net PnL after open and close taker fees, mirroring
     * {@code PaperTradingService} accounting. Treated as a loss when the paired ENTRY is missing.
     */
    private static boolean isReverseWin(JobTimelineEvent entry, JobTimelineEvent exit, double takerFeeRate) {
        if (entry == null) {
            return false;
        }
        double qty = exit.quantity();
        double grossPnl = "BUY".equalsIgnoreCase(entry.side())
                ? (exit.price() - entry.price()) * qty
                : (entry.price() - exit.price()) * qty;
        double openFee = entry.price() * entry.quantity() * takerFeeRate;
        double closeFee = exit.price() * qty * takerFeeRate;
        return grossPnl - closeFee - openFee >= 0;
    }

    /**
     * Prefer OHLC from {@code strategy.backtest_kline} for the job's effective range (same rows strategy uses when
     * evaluating). If the job id is not a UUID, the job row is missing, or klines are absent, fall back to mark replay
     * points in {@code simulate.job_candle}.
     */
    private List<JobTimelineCandle> loadChartCandlesFromStrategyOrFallback(String jobId) {
        Optional<UUID> jobUuid = parseUuid(jobId);
        if (jobUuid.isPresent()) {
            Optional<BacktestJobKlineRange> rangeOpt =
                    strategyBacktestChartDataRepository.findJobKlineRange(jobUuid.get());
            if (rangeOpt.isPresent() && rangeOpt.get().isComplete()) {
                BacktestJobKlineRange r = rangeOpt.get();
                List<JobTimelineCandle> fromStrategy = strategyBacktestChartDataRepository.findKlinesInRange(
                        r.symbol(), r.klineInterval(), r.effectiveStartMs(), r.effectiveEndMs());
                if (!fromStrategy.isEmpty()) {
                    return fromStrategy;
                }
            }
        }
        return jobCandleRepository.findByJobIdOrderByTsAsc(jobId).stream()
                .map(c -> new JobTimelineCandle(c.getTs(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume(), 0.0, 0.0))
                .toList();
    }

    private static Optional<UUID> parseUuid(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jobId.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
