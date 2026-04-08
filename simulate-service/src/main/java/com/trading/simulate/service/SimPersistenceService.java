package com.trading.simulate.service;

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
import java.time.Instant;
import java.util.List;
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

    public SimPersistenceService(
            PaperAccountSnapshotRepository snapshotRepository,
            PaperOrderRepository orderRepository,
            PaperPositionRepository positionRepository,
            PaperFillRepository fillRepository,
            JobCandleRepository jobCandleRepository,
            JobTradeEventRepository jobTradeEventRepository,
            JobBalanceSnapshotRepository jobBalanceSnapshotRepository) {
        this.snapshotRepository = snapshotRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
        this.jobCandleRepository = jobCandleRepository;
        this.jobTradeEventRepository = jobTradeEventRepository;
        this.jobBalanceSnapshotRepository = jobBalanceSnapshotRepository;
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
        List<JobTimelineCandle> candles = jobCandleRepository.findByJobIdOrderByTsAsc(jobId).stream()
                .map(c -> new JobTimelineCandle(c.getTs(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()))
                .toList();
        List<JobTimelineEvent> events = jobTradeEventRepository.findByJobIdOrderByEventTimeAsc(jobId).stream()
                .map(e -> new JobTimelineEvent(e.getEventType(), e.getSide(), e.getPrice(), e.getQuantity(), e.getTp(), e.getSl(), e.getEventTime()))
                .toList();
        List<JobTimelineBalance> balance = jobBalanceSnapshotRepository.findByJobIdOrderByEventTimeAsc(jobId).stream()
                .map(b -> new JobTimelineBalance(b.getEventTime(), b.getBalanceUsdt()))
                .toList();
        int wins = (int) events.stream().filter(e -> "TP".equals(e.type())).count();
        int losses = (int) events.stream().filter(e -> "SL".equals(e.type())).count();
        int liquidations = (int) events.stream().filter(e -> "LIQUIDATED".equals(e.type())).count();
        int totalTrades = wins + losses + liquidations;
        double totalPnl = 0.0;
        if (balance.size() >= 2) {
            totalPnl = balance.get(balance.size() - 1).balanceUsdt() - balance.get(0).balanceUsdt();
        }
        JobTimelineSummary summary = new JobTimelineSummary(
                wins,
                losses,
                liquidations,
                totalTrades,
                totalPnl,
                0.0);
        return new JobTimeline(jobId, candles, events, balance, summary);
    }
}
