package com.trading.simulate.service;

import com.trading.simulate.model.PaperAccountState;
import com.trading.simulate.model.PaperPosition;
import com.trading.simulate.model.PaperStats;
import com.trading.simulate.persistence.entity.PaperAccountSnapshotEntity;
import com.trading.simulate.persistence.entity.PaperFillEntity;
import com.trading.simulate.persistence.entity.PaperOrderEntity;
import com.trading.simulate.persistence.entity.PaperPositionEntity;
import com.trading.simulate.persistence.repository.PaperAccountSnapshotRepository;
import com.trading.simulate.persistence.repository.PaperFillRepository;
import com.trading.simulate.persistence.repository.PaperOrderRepository;
import com.trading.simulate.persistence.repository.PaperPositionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SimPersistenceService {
    private final PaperAccountSnapshotRepository snapshotRepository;
    private final PaperOrderRepository orderRepository;
    private final PaperPositionRepository positionRepository;
    private final PaperFillRepository fillRepository;

    public SimPersistenceService(
            PaperAccountSnapshotRepository snapshotRepository,
            PaperOrderRepository orderRepository,
            PaperPositionRepository positionRepository,
            PaperFillRepository fillRepository) {
        this.snapshotRepository = snapshotRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
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

    public void savePosition(PaperPosition p, String status) {
        PaperPositionEntity e = new PaperPositionEntity();
        e.setSymbol(p.getSymbol());
        e.setSide(p.getSide());
        e.setQuantity(p.getQuantity());
        e.setEntryPrice(p.getEntryPrice());
        e.setTakeProfitPrice(p.getTakeProfitPrice());
        e.setStopLossPrice(p.getStopLossPrice());
        e.setStatus(status);
        e.setOpenedAt(Instant.now());
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
}
