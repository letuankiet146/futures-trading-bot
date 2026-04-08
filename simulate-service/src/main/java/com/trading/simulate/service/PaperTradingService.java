package com.trading.simulate.service;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.config.SimulateProperties;
import com.trading.simulate.model.JobTimeline;
import com.trading.simulate.model.PaperAccountState;
import com.trading.simulate.model.PaperPosition;
import com.trading.simulate.model.PaperStats;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaperTradingService {
    private static final Logger log = LoggerFactory.getLogger(PaperTradingService.class);
    private final SimulateProperties properties;
    private final SimPersistenceService persistenceService;
    private final SimAuditService auditService;
    private final Object lock = new Object();
    private PaperAccountState state;

    public PaperTradingService(
            SimulateProperties properties,
            SimPersistenceService persistenceService,
            SimAuditService auditService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        synchronized (lock) {
            resetInternal();
        }
    }

    public void onStrategySignal(StrategySignalEvent event) {
        synchronized (lock) {
            if (state.isFrozen()) {
                log.warn("Paper account is frozen. Ignore signal correlationId={}", event.getCorrelationId());
                auditService.log("FROZEN_SKIP", event.getCorrelationId(), "Signal ignored because account is frozen");
                return;
            }
            String jobId = normalizeJobId(event.getCorrelationId());
            if (jobId == null) {
                log.warn("Missing correlationId in strategy signal, skip event");
                auditService.log("OPEN_REJECTED", null, "Missing correlationId");
                return;
            }
            Instant eventTime = parseEventTime(event.getTimestamp());
            onMarkPriceInternal(event.getPrice(), jobId, eventTime);
            if (state.getOpenPosition() != null && state.getOpenPosition().isActive()) {
                // Single active trade cycle in v1: ignore new entries while a position is open.
                return;
            }
            openPosition(event.getSymbol(), event.getSide(), event.getPrice(), jobId, eventTime);
        }
    }

    public void onMarkPrice(double markPrice) {
        synchronized (lock) {
            String activeJobId = state.getOpenPosition() == null ? null : state.getOpenPosition().getJobId();
            onMarkPriceInternal(markPrice, activeJobId, Instant.now());
        }
    }

    public void onReplayMarkPrice(double markPrice, String correlationId, String timestamp) {
        synchronized (lock) {
            String jobId = normalizeJobId(correlationId);
            if (jobId == null) {
                log.warn("Missing correlationId in replay MARK, skip");
                return;
            }
            onMarkPriceInternal(markPrice, jobId, parseEventTime(timestamp));
        }
    }

    public PaperAccountState snapshot() {
        synchronized (lock) {
            return copyState(state);
        }
    }

    public void reset() {
        synchronized (lock) {
            resetInternal();
        }
    }

    public JobTimeline timeline(String jobId) {
        synchronized (lock) {
            return persistenceService.loadTimeline(jobId);
        }
    }

    private void openPosition(String symbol, String side, double entryMark, String jobId, Instant eventTime) {
        double notional = resolveNotional(state.getBalanceUsdt());
        if (notional <= 0 || notional > state.getBalanceUsdt()) {
            log.warn("Insufficient balance for opening simulated order. balance={} notional={}", state.getBalanceUsdt(), notional);
            auditService.log("OPEN_REJECTED", jobId, "Insufficient balance");
            return;
        }
        double qty = notional / entryMark;
        double margin = notional / Math.max(1, properties.getLeverage());
        double feeOpen = notional * properties.getTakerFee();

        double distanceRate = 4 * properties.getTakerFee();
        double tp;
        double sl;
        if ("BUY".equalsIgnoreCase(side)) {
            tp = entryMark * (1 + distanceRate);
            sl = entryMark * (1 - distanceRate);
        } else {
            tp = entryMark * (1 - distanceRate);
            sl = entryMark * (1 + distanceRate);
        }

        PaperPosition position = new PaperPosition();
        position.setActive(true);
        position.setSide(side.toUpperCase());
        position.setSymbol(symbol);
        position.setEntryPrice(entryMark);
        position.setQuantity(qty);
        position.setNotional(notional);
        position.setIsolatedMargin(margin);
        position.setTakeProfitPrice(tp);
        position.setStopLossPrice(sl);
        position.setOpenFee(feeOpen);
        position.setJobId(jobId);
        position.setOpenedAt(eventTime);

        state.setOpenPosition(position);
        state.setBalanceUsdt(state.getBalanceUsdt() - feeOpen);
        state.getStats().setTotalFees(state.getStats().getTotalFees() + feeOpen);
        persistenceService.saveOrder(symbol, side.toUpperCase(), qty, entryMark, "FILLED", jobId);
        persistenceService.savePosition(position, "OPEN", jobId);
        persistenceService.saveFill(symbol, side.toUpperCase(), qty, entryMark, "ENTRY");
        persistenceService.saveTradeEvent(jobId, "ENTRY", side.toUpperCase(), entryMark, qty, tp, sl, eventTime);
        persistenceService.saveJobBalance(jobId, state.getBalanceUsdt(), eventTime);
        persistenceService.saveSnapshot(state);
        auditService.log("POSITION_OPENED", jobId, "Paper position opened");
        log.info("Opened paper position side={} symbol={} qty={} entry={} tp={} sl={}",
                side, symbol, qty, entryMark, tp, sl);
    }

    private void closePosition(double closeMark, String reason, Instant eventTime) {
        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive()) {
            return;
        }
        double pnl = computePnl(pos, closeMark);
        double closeNotional = pos.getQuantity() * closeMark;
        double feeClose = closeNotional * properties.getTakerFee();
        double net = pnl - feeClose;
        double netAfterAllFees = net - pos.getOpenFee();
        state.setBalanceUsdt(state.getBalanceUsdt() + net);
        pos.setActive(false);
        state.setOpenPosition(null);

        PaperStats stats = state.getStats();
        stats.setTotalTrades(stats.getTotalTrades() + 1);
        stats.setTotalPnl(stats.getTotalPnl() + netAfterAllFees);
        stats.setTotalFees(stats.getTotalFees() + feeClose);
        if ("TP".equals(reason)) {
            stats.setWinCount(stats.getWinCount() + 1);
        } else {
            stats.setLoseCount(stats.getLoseCount() + 1);
        }
        persistenceService.saveFill(pos.getSymbol(), pos.getSide(), pos.getQuantity(), closeMark, reason);
        persistenceService.saveTradeEvent(
                pos.getJobId(),
                reason,
                pos.getSide(),
                closeMark,
                pos.getQuantity(),
                pos.getTakeProfitPrice(),
                pos.getStopLossPrice(),
                eventTime);
        persistenceService.saveJobBalance(pos.getJobId(), state.getBalanceUsdt(), eventTime);
        persistenceService.saveSnapshot(state);
        auditService.log("POSITION_CLOSED", pos.getJobId(), "Closed by " + reason);
        log.info("Closed paper position reason={} close={} pnl={} fee={} net={}", reason, closeMark, pnl, feeClose, net);
    }

    private void liquidate(double markPrice, Instant eventTime) {
        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive()) {
            return;
        }
        double pnl = computePnl(pos, markPrice);
        double realizedLoss = Math.max(0.0, -pnl);
        state.setBalanceUsdt(state.getBalanceUsdt() - realizedLoss);
        pos.setActive(false);
        state.setOpenPosition(null);
        state.setFrozen(true);

        PaperStats stats = state.getStats();
        stats.setTotalTrades(stats.getTotalTrades() + 1);
        stats.setTotalPnl(stats.getTotalPnl() - (realizedLoss + pos.getOpenFee()));
        stats.setLiquidationCount(stats.getLiquidationCount() + 1);
        persistenceService.saveFill(pos.getSymbol(), pos.getSide(), pos.getQuantity(), markPrice, "LIQUIDATED");
        persistenceService.saveTradeEvent(
                pos.getJobId(),
                "LIQUIDATED",
                pos.getSide(),
                markPrice,
                pos.getQuantity(),
                pos.getTakeProfitPrice(),
                pos.getStopLossPrice(),
                eventTime);
        persistenceService.saveJobBalance(pos.getJobId(), state.getBalanceUsdt(), eventTime);
        persistenceService.saveSnapshot(state);
        auditService.log("LIQUIDATION", pos.getJobId(), "Paper liquidation and freeze");
        log.warn("Paper liquidation occurred at mark={} realizedLoss={} account frozen until reset", markPrice, realizedLoss);
    }

    private double computePnl(PaperPosition pos, double markPrice) {
        if ("BUY".equalsIgnoreCase(pos.getSide())) {
            return (markPrice - pos.getEntryPrice()) * pos.getQuantity();
        }
        return (pos.getEntryPrice() - markPrice) * pos.getQuantity();
    }

    private double resolveNotional(double balance) {
        SimulateProperties.Sizing s = properties.getSizing();
        if ("FIXED".equalsIgnoreCase(s.getMode())) {
            return s.getFixedNotionalUsdt();
        }
        double pct = Math.min(s.getAccountPercent(), s.getMaxAccountPercent());
        return balance * pct;
    }

    private void resetInternal() {
        PaperAccountState newState = new PaperAccountState();
        newState.setBalanceUsdt(properties.getInitialBalanceUsdt());
        newState.setLastMarkPrice(null);
        newState.setFrozen(false);
        newState.setOpenPosition(null);
        newState.setStats(new PaperStats());
        this.state = newState;
        persistenceService.saveSnapshot(state);
        auditService.log("RESET", null, "Paper account reset");
    }

    private PaperAccountState copyState(PaperAccountState src) {
        PaperAccountState dst = new PaperAccountState();
        dst.setBalanceUsdt(src.getBalanceUsdt());
        dst.setLastMarkPrice(src.getLastMarkPrice());
        dst.setFrozen(src.isFrozen());
        if (src.getOpenPosition() != null) {
            PaperPosition p = src.getOpenPosition();
            PaperPosition cp = new PaperPosition();
            cp.setSide(p.getSide());
            cp.setSymbol(p.getSymbol());
            cp.setEntryPrice(p.getEntryPrice());
            cp.setQuantity(p.getQuantity());
            cp.setNotional(p.getNotional());
            cp.setIsolatedMargin(p.getIsolatedMargin());
            cp.setTakeProfitPrice(p.getTakeProfitPrice());
            cp.setStopLossPrice(p.getStopLossPrice());
            cp.setOpenFee(p.getOpenFee());
            cp.setJobId(p.getJobId());
            cp.setOpenedAt(p.getOpenedAt());
            cp.setActive(p.isActive());
            dst.setOpenPosition(cp);
        }
        PaperStats s = src.getStats();
        PaperStats sc = new PaperStats();
        sc.setWinCount(s.getWinCount());
        sc.setLoseCount(s.getLoseCount());
        sc.setLiquidationCount(s.getLiquidationCount());
        sc.setTotalTrades(s.getTotalTrades());
        sc.setTotalPnl(s.getTotalPnl());
        sc.setTotalFees(s.getTotalFees());
        dst.setStats(sc);
        return dst;
    }

    private void onMarkPriceInternal(double markPrice, String jobId, Instant eventTime) {
        state.setLastMarkPrice(markPrice);
        if (jobId != null) {
            persistenceService.saveJobCandle(jobId, eventTime, markPrice);
        }
        if (state.isFrozen()) {
            return;
        }
        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive()) {
            return;
        }

        if ("BUY".equalsIgnoreCase(pos.getSide())) {
            if (markPrice >= pos.getTakeProfitPrice()) {
                closePosition(markPrice, "TP", eventTime);
                return;
            }
            if (markPrice <= pos.getStopLossPrice()) {
                closePosition(markPrice, "SL", eventTime);
                return;
            }
        } else {
            if (markPrice <= pos.getTakeProfitPrice()) {
                closePosition(markPrice, "TP", eventTime);
                return;
            }
            if (markPrice >= pos.getStopLossPrice()) {
                closePosition(markPrice, "SL", eventTime);
                return;
            }
        }

        double unrealizedLoss = Math.max(0.0, -computePnl(pos, markPrice));
        double trigger = pos.getIsolatedMargin() * properties.getLiquidation().getIsolatedMarginLossThreshold();
        if (unrealizedLoss >= trigger) {
            liquidate(markPrice, eventTime);
        }
    }

    private String normalizeJobId(String rawCorrelationId) {
        if (rawCorrelationId == null || rawCorrelationId.isBlank()) {
            return null;
        }
        return rawCorrelationId.trim();
    }

    private Instant parseEventTime(String rawTs) {
        if (rawTs == null || rawTs.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(rawTs);
        } catch (DateTimeParseException ex) {
            return Instant.now();
        }
    }
}
