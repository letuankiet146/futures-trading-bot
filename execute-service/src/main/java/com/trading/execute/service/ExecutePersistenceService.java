package com.trading.execute.service;

import com.trading.execute.persistence.entity.LiveFillEntity;
import com.trading.execute.persistence.entity.LiveOrderEntity;
import com.trading.execute.persistence.entity.LivePositionEntity;
import com.trading.execute.persistence.repository.LiveFillRepository;
import com.trading.execute.persistence.repository.LiveOrderRepository;
import com.trading.execute.persistence.repository.LivePositionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ExecutePersistenceService {
    private final LiveOrderRepository orderRepository;
    private final LivePositionRepository positionRepository;
    private final LiveFillRepository fillRepository;

    public ExecutePersistenceService(
            LiveOrderRepository orderRepository,
            LivePositionRepository positionRepository,
            LiveFillRepository fillRepository) {
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.fillRepository = fillRepository;
    }

    public void saveEntryOrder(String symbol, String side, double quantity, String clientOrderId, String correlationId) {
        LiveOrderEntity o = new LiveOrderEntity();
        o.setSymbol(symbol);
        o.setSide(side);
        o.setType("MARKET");
        o.setQuantity(quantity);
        o.setClientOrderId(clientOrderId);
        o.setCorrelationId(correlationId);
        o.setStatus("SUBMITTED");
        o.setCreatedAt(Instant.now());
        orderRepository.save(o);
    }

    public void saveBracketOrder(String symbol, String side, double quantity, String clientOrderId, String type, String correlationId) {
        LiveOrderEntity o = new LiveOrderEntity();
        o.setSymbol(symbol);
        o.setSide(side);
        o.setType(type);
        o.setQuantity(quantity);
        o.setClientOrderId(clientOrderId);
        o.setCorrelationId(correlationId);
        o.setStatus("SUBMITTED");
        o.setCreatedAt(Instant.now());
        orderRepository.save(o);
    }

    public void openPosition(String symbol, String side, double quantity, double entryPrice) {
        LivePositionEntity p = new LivePositionEntity();
        p.setSymbol(symbol);
        p.setSide(side);
        p.setQuantity(quantity);
        p.setEntryPrice(entryPrice);
        p.setStatus("OPEN");
        p.setOpenedAt(Instant.now());
        positionRepository.save(p);
    }

    public void saveFill(String symbol, String side, double quantity, double price, String fillType) {
        LiveFillEntity f = new LiveFillEntity();
        f.setSymbol(symbol);
        f.setSide(side);
        f.setQuantity(quantity);
        f.setPrice(price);
        f.setFillType(fillType);
        f.setFillTime(Instant.now());
        fillRepository.save(f);
    }
}
