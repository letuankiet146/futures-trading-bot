package com.trading.simulate.service;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.backtest.BacktestKlineDrillClient;
import com.trading.simulate.backtest.ReplayCandle;
import com.trading.simulate.config.SimulateProperties;
import com.trading.simulate.model.JobTimeline;
import com.trading.simulate.model.PaperAccountState;
import com.trading.simulate.model.PaperPosition;
import com.trading.simulate.model.PaperStats;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaperTradingService {
    private static final Logger log = LoggerFactory.getLogger(PaperTradingService.class);
    private final SimulateProperties properties;
    private final SimPersistenceService persistenceService;
    private final SimAuditService auditService;
    private final BacktestKlineDrillClient drillClient;
    private final Object lock = new Object();
    private PaperAccountState state;

    public PaperTradingService(
            SimulateProperties properties,
            SimPersistenceService persistenceService,
            SimAuditService auditService,
            BacktestKlineDrillClient drillClient) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
        this.drillClient = drillClient;
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
            PaperPosition active = state.getOpenPosition();
            if (active != null && active.isActive()) {
                String signalSide = event.getSide() == null ? "" : event.getSide().trim().toUpperCase();
                String activeSide = active.getSide() == null ? "" : active.getSide().trim().toUpperCase();
                if (!signalSide.isBlank() && !activeSide.isBlank() && !activeSide.equals(signalSide)) {
                    // Reverse-on-signal: close current leg first (effectively cancels its TP/SL), then open new side.
                    auditService.log("POSITION_REVERSED", jobId, "Reverse signal closes current position before re-entry");
                    closePosition(event.getPrice(), "REVERSE", eventTime);
                } else {
                    // Same-side signal while position is active: average in (DCA) if enabled, otherwise ignore.
                    if (properties.getAveraging().isEnabled()) {
                        averageIntoPosition(
                                event.getSymbol(),
                                event.getPrice(),
                                event.getTakeProfitPrice(),
                                event.getStopLossPrice(),
                                jobId,
                                eventTime);
                    }
                    return;
                }
            }
            openPosition(
                    event.getSymbol(),
                    event.getSide(),
                    event.getPrice(),
                    event.getTakeProfitPrice(),
                    event.getStopLossPrice(),
                    jobId,
                    eventTime);
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

    /**
     * Replay a full candle. When an open position can touch both TP and SL inside this candle, drill down
     * into finer intervals (fetched/cached via strategy-service) to learn which one is hit first.
     */
    public void onReplayCandle(String symbol, ReplayCandle candle, String correlationId) {
        synchronized (lock) {
            String jobId = normalizeJobId(correlationId);
            if (jobId == null) {
                log.warn("Missing correlationId in replay CANDLE, skip");
                return;
            }
            resolveCandle(symbol, candle, jobId);
        }
    }

    /** Must be called while holding {@link #lock}. */
    private void resolveCandle(String symbol, ReplayCandle candle, String jobId) {
        Instant closeTime = Instant.ofEpochMilli(candle.closeTimeMs());

        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive() || state.isFrozen()) {
            // Nothing to resolve. Chart data is served from strategy klines, so no per-candle persistence here;
            // just keep the in-memory mark for continuity.
            state.setLastMarkPrice(candle.close());
            return;
        }

        boolean buy = "BUY".equalsIgnoreCase(pos.getSide());
        Double tp = pos.getTakeProfitPrice();
        Double sl = pos.getStopLossPrice();
        // A null leg (no TP / no SL) is never reachable; the position then only closes via liquidation or reverse.
        boolean tpReachable = tp != null && (buy ? candle.high() >= tp : candle.low() <= tp);
        boolean slReachable = sl != null && (buy ? candle.low() <= sl : candle.high() >= sl);

        if (!tpReachable && !slReachable) {
            // No TP/SL exit this candle; the position only closes if the adverse extreme triggers liquidation.
            double adverseExtreme = buy ? candle.low() : candle.high();
            if (checkLiquidation(adverseExtreme, closeTime)) {
                return;
            }
            state.setLastMarkPrice(candle.close());
            return;
        }

        if (tpReachable ^ slReachable) {
            // Unambiguous: exit at the exact bracket price.
            if (tpReachable) {
                closePosition(tp, "TP", closeTime);
            } else {
                closePosition(sl, "SL", closeTime);
            }
            return;
        }

        // Ambiguous: this candle can touch both TP and SL. Drill into a finer interval to order them.
        List<ReplayCandle> finer = drillClient.fetchFiner(symbol, candle);
        if (finer.isEmpty()) {
            boolean slFirst = !"TP".equalsIgnoreCase(properties.getBacktest().getTieBreak());
            log.debug("Drill reached finest interval={} for jobId={}; tie-break {}",
                    candle.interval(), jobId, slFirst ? "SL" : "TP");
            if (slFirst) {
                closePosition(sl, "SL", closeTime);
            } else {
                closePosition(tp, "TP", closeTime);
            }
            return;
        }
        for (ReplayCandle sub : finer) {
            if (positionClosed()) {
                state.setLastMarkPrice(sub.close());
            } else {
                resolveCandle(symbol, sub, jobId);
            }
        }
    }

    private boolean positionClosed() {
        PaperPosition pos = state.getOpenPosition();
        return pos == null || !pos.isActive();
    }

    /**
     * Liquidation-only mark check used during candle replay. Unlike {@link #onMarkPriceInternal} it does not
     * persist per-candle chart rows and does not evaluate TP/SL (those are decided by {@link #resolveCandle}).
     *
     * @return true if the position was liquidated
     */
    private boolean checkLiquidation(double markPrice, Instant eventTime) {
        state.setLastMarkPrice(markPrice);
        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive() || state.isFrozen()) {
            return false;
        }
        double unrealizedLoss = Math.max(0.0, -computePnl(pos, markPrice));
        SimulateProperties.Liquidation liq = properties.getLiquidation();
        // ISOLATED: only the position margin backs the trade. CROSS: the whole wallet backs it.
        double collateral = liq.isCross() ? state.getBalanceUsdt() : pos.getIsolatedMargin();
        double trigger = collateral * liq.getIsolatedMarginLossThreshold();
        if (unrealizedLoss >= trigger) {
            liquidate(markPrice, eventTime);
            return true;
        }
        return false;
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

    /** Resets the paper account at the start of a backtest job so each replay is isolated. */
    public void onReplayReset(String jobId) {
        synchronized (lock) {
            resetInternal();
            log.info("SIMULATE replay RESET paper account for backtest jobId={}", jobId);
        }
    }

    public JobTimeline timeline(String jobId) {
        synchronized (lock) {
            return persistenceService.loadTimeline(jobId);
        }
    }

    private void openPosition(
            String symbol,
            String side,
            double entryMark,
            Double takeProfitPrice,
            Double stopLossPrice,
            String jobId,
            Instant eventTime) {
        if (!isValidBracket(side, entryMark, takeProfitPrice, stopLossPrice)) {
            // null TP/SL is allowed (no bracket); this only rejects a present leg on the wrong side of entry.
            log.warn("Reject open: invalid TP/SL placement jobId={} side={} entry={} tp={} sl={}",
                    jobId, side, entryMark, takeProfitPrice, stopLossPrice);
            auditService.log("OPEN_REJECTED", jobId, "Invalid strategy TP/SL placement");
            return;
        }
        // null TP/SL means the position has no take-profit / stop-loss leg.
        Double tp = takeProfitPrice;
        Double sl = stopLossPrice;
        double allocatedUsdt = resolveAllocatedUsdt(state.getBalanceUsdt());
        if (allocatedUsdt <= 0 || allocatedUsdt > state.getBalanceUsdt()) {
            log.warn("Insufficient balance for opening simulated order. balance={} allocatedUsdt={}",
                    state.getBalanceUsdt(), allocatedUsdt);
            auditService.log("OPEN_REJECTED", jobId, "Insufficient balance");
            return;
        }
        // Position size is 1:1 with allocated USDT — no leverage in sizing.
        double notional = allocatedUsdt;
        double qty = notional / entryMark;
        double margin = notional;
        double feeOpen = notional * properties.getTakerFee();

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
        position.setEntryCount(1);

        state.setOpenPosition(position);
        state.setBalanceUsdt(state.getBalanceUsdt() - feeOpen);
        state.getStats().setTotalFees(state.getStats().getTotalFees() + feeOpen);
        persistenceService.saveOrder(symbol, side.toUpperCase(), qty, entryMark, "FILLED", jobId);
        persistenceService.savePosition(position, "OPEN", jobId);
        persistenceService.saveFill(symbol, side.toUpperCase(), qty, entryMark, "ENTRY");
        persistenceService.saveTradeEvent(
                jobId, "ENTRY", side.toUpperCase(), entryMark, qty, takeProfitPrice, stopLossPrice, eventTime);
        persistenceService.saveJobBalance(jobId, state.getBalanceUsdt(), eventTime);
        persistenceService.saveSnapshot(state);
        auditService.log("POSITION_OPENED", jobId, "Paper position opened");
        log.info("Opened paper position side={} symbol={} qty={} entry={} allocatedUsdt={} tp={} sl={}",
                side, symbol, formatQty(qty), entryMark, notional, tp, sl);
    }

    /**
     * Adds a same-side leg to the open position (DCA): recomputes the size-weighted average entry, the
     * aggregate notional/margin, and re-centers TP/SL at the new average entry preserving the new signal's
     * percent distances.
     */
    private void averageIntoPosition(
            String symbol,
            double addMark,
            Double takeProfitPrice,
            Double stopLossPrice,
            String jobId,
            Instant eventTime) {
        PaperPosition pos = state.getOpenPosition();
        if (pos == null || !pos.isActive()) {
            return;
        }
        int maxEntries = properties.getAveraging().getMaxEntries();
        if (maxEntries > 0 && pos.getEntryCount() >= maxEntries) {
            auditService.log("AVERAGE_SKIPPED", jobId, "Max averaging entries reached: " + maxEntries);
            return;
        }
        if (!isValidBracket(pos.getSide(), addMark, takeProfitPrice, stopLossPrice)) {
            log.warn("Reject average: invalid TP/SL jobId={} side={} mark={} tp={} sl={}",
                    jobId, pos.getSide(), addMark, takeProfitPrice, stopLossPrice);
            auditService.log("AVERAGE_REJECTED", jobId, "Missing or invalid strategy TP/SL");
            return;
        }
        double addNotional = resolveAllocatedUsdt(state.getBalanceUsdt());
        if (addNotional <= 0 || addNotional > state.getBalanceUsdt()) {
            log.warn("Insufficient balance to average. balance={} addNotional={}", state.getBalanceUsdt(), addNotional);
            auditService.log("AVERAGE_REJECTED", jobId, "Insufficient balance");
            return;
        }

        double addQty = addNotional / addMark;
        double newQty = pos.getQuantity() + addQty;
        double totalCost = pos.getQuantity() * pos.getEntryPrice() + addQty * addMark;
        double newAvgEntry = totalCost / newQty;
        double newNotional = totalCost; // == newQty * newAvgEntry
        // Position size is 1:1 with allocated USDT (no leverage in sizing), matching openPosition.
        double newMargin = newNotional;
        double feeOpen = addNotional * properties.getTakerFee();
        // Re-apply the new signal's TP/SL percent distances (relative to the signal price) onto the new
        // average entry, preserving disabled (null) legs.
        Double[] bracket = recenterBracket(
                pos.getSide(), addMark, newAvgEntry, takeProfitPrice, stopLossPrice);

        pos.setQuantity(newQty);
        pos.setEntryPrice(newAvgEntry);
        pos.setNotional(newNotional);
        pos.setIsolatedMargin(newMargin);
        pos.setTakeProfitPrice(bracket[0]);
        pos.setStopLossPrice(bracket[1]);
        pos.setOpenFee(pos.getOpenFee() + feeOpen);
        pos.setEntryCount(pos.getEntryCount() + 1);

        state.setBalanceUsdt(state.getBalanceUsdt() - feeOpen);
        state.getStats().setTotalFees(state.getStats().getTotalFees() + feeOpen);

        persistenceService.saveOrder(symbol, pos.getSide(), addQty, addMark, "FILLED", jobId);
        persistenceService.savePosition(pos, "AVERAGE", jobId);
        persistenceService.saveFill(symbol, pos.getSide(), addQty, addMark, "ENTRY");
        persistenceService.saveTradeEvent(
                jobId, "AVERAGE", pos.getSide(), addMark, addQty, bracket[0], bracket[1], eventTime);
        persistenceService.saveJobBalance(jobId, state.getBalanceUsdt(), eventTime);
        persistenceService.saveSnapshot(state);
        auditService.log("POSITION_AVERAGED", jobId, "Averaged into existing position (leg " + pos.getEntryCount() + ")");
        log.info("Averaged position side={} addQty={} addMark={} newQty={} newAvgEntry={} tp={} sl={} legs={}",
                pos.getSide(), formatQty(addQty), addMark, formatQty(newQty), newAvgEntry,
                bracket[0], bracket[1], pos.getEntryCount());
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
        } else if ("REVERSE".equals(reason)) {
            if (netAfterAllFees >= 0) {
                stats.setWinCount(stats.getWinCount() + 1);
            } else {
                stats.setLoseCount(stats.getLoseCount() + 1);
            }
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
        // Forced close at the liquidation mark. The realized loss is bounded by the position margin
        // in ISOLATED mode and by the whole wallet in CROSS mode (via the trigger in onMarkPriceInternal).
        double pnl = computePnl(pos, markPrice);
        double closeNotional = pos.getQuantity() * markPrice;
        double feeClose = closeNotional * properties.getTakerFee();
        double net = pnl - feeClose;
        double netAfterAllFees = net - pos.getOpenFee();
        state.setBalanceUsdt(Math.max(0.0, state.getBalanceUsdt() + net));
        pos.setActive(false);
        state.setOpenPosition(null);

        PaperStats stats = state.getStats();
        stats.setTotalTrades(stats.getTotalTrades() + 1);
        stats.setTotalPnl(stats.getTotalPnl() + netAfterAllFees);
        stats.setTotalFees(stats.getTotalFees() + feeClose);
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
        String mode = properties.getLiquidation().isCross() ? "CROSS" : "ISOLATED";
        auditService.log("LIQUIDATION", pos.getJobId(), "Paper " + mode + " liquidation");
        log.warn("Paper {} liquidation at mark={} realizedNet={} account remains active",
                mode, markPrice, net);
    }

    /** Re-applies bracket distances onto a new entry, preserving disabled ({@code null}) legs. */
    private static Double[] recenterBracket(
            String side, double referencePrice, double entryPrice, Double takeProfitPrice, Double stopLossPrice) {
        boolean buy = "BUY".equalsIgnoreCase(side);
        Double tp = null;
        Double sl = null;
        if (takeProfitPrice != null) {
            double tpRate = buy
                    ? (takeProfitPrice - referencePrice) / referencePrice
                    : (referencePrice - takeProfitPrice) / referencePrice;
            tp = buy ? entryPrice * (1 + tpRate) : entryPrice * (1 - tpRate);
        }
        if (stopLossPrice != null) {
            double slRate = buy
                    ? (referencePrice - stopLossPrice) / referencePrice
                    : (stopLossPrice - referencePrice) / referencePrice;
            sl = buy ? entryPrice * (1 - slRate) : entryPrice * (1 + slRate);
        }
        return new Double[] {tp, sl};
    }

    private double computePnl(PaperPosition pos, double markPrice) {
        if ("BUY".equalsIgnoreCase(pos.getSide())) {
            return (markPrice - pos.getEntryPrice()) * pos.getQuantity();
        }
        return (pos.getEntryPrice() - markPrice) * pos.getQuantity();
    }

    /** USDT from wallet used for this trade (balance × sizing percent, or fixed amount). */
    private double resolveAllocatedUsdt(double balance) {
        SimulateProperties.Sizing s = properties.getSizing();
        if ("FIXED".equalsIgnoreCase(s.getMode())) {
            return s.getFixedNotionalUsdt();
        }
        double pct = Math.min(s.getAccountPercent(), s.getMaxAccountPercent());
        return balance * pct;
    }

    /**
     * Validates the entry and any present TP/SL. A {@code null} TP or SL means "no take-profit" / "no
     * stop-loss" and is allowed; a present leg must sit on the correct side of the entry.
     */
    private static boolean isValidBracket(String side, double entry, Double tp, Double sl) {
        if (entry <= 0) {
            return false;
        }
        boolean buy = "BUY".equalsIgnoreCase(side);
        boolean sell = "SELL".equalsIgnoreCase(side);
        if (!buy && !sell) {
            return false;
        }
        if (tp != null) {
            if (tp <= 0 || (buy ? tp <= entry : tp >= entry)) {
                return false;
            }
        }
        if (sl != null) {
            if (sl <= 0 || (buy ? sl >= entry : sl <= entry)) {
                return false;
            }
        }
        return true;
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
            cp.setEntryCount(p.getEntryCount());
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

        // A null TP/SL leg means there is no take-profit / stop-loss; skip that check.
        Double tp = pos.getTakeProfitPrice();
        Double sl = pos.getStopLossPrice();
        if ("BUY".equalsIgnoreCase(pos.getSide())) {
            if (tp != null && markPrice >= tp) {
                closePosition(markPrice, "TP", eventTime);
                return;
            }
            if (sl != null && markPrice <= sl) {
                closePosition(markPrice, "SL", eventTime);
                return;
            }
        } else {
            if (tp != null && markPrice <= tp) {
                closePosition(markPrice, "TP", eventTime);
                return;
            }
            if (sl != null && markPrice >= sl) {
                closePosition(markPrice, "SL", eventTime);
                return;
            }
        }

        double unrealizedLoss = Math.max(0.0, -computePnl(pos, markPrice));
        SimulateProperties.Liquidation liq = properties.getLiquidation();
        // ISOLATED: only the position margin backs the trade. CROSS: the whole wallet backs it,
        // so the loss must consume the threshold fraction of total balance before liquidation.
        double collateral = liq.isCross() ? state.getBalanceUsdt() : pos.getIsolatedMargin();
        double trigger = collateral * liq.getIsolatedMarginLossThreshold();
        if (unrealizedLoss >= trigger) {
            liquidate(markPrice, eventTime);
        }
    }

    private static String formatQty(double qty) {
        return String.format(Locale.US, "%.8f", qty);
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
