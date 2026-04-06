package com.trading.execute.service;

import com.trading.execute.config.ExecuteProperties;
import com.trading.execute.model.RiskBucketState;
import com.trading.execute.persistence.entity.RiskBucketStateEntity;
import com.trading.execute.persistence.repository.RiskBucketStateRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class RiskBucketService {
    private final ExecuteProperties executeProperties;
    private final RiskBucketStateRepository repository;
    private final Object lock = new Object();
    private RiskBucketState state;

    public RiskBucketService(ExecuteProperties executeProperties, RiskBucketStateRepository repository) {
        this.executeProperties = executeProperties;
        this.repository = repository;
    }

    public boolean isAllowed(double equityNow) {
        synchronized (lock) {
            initOrRollIfNeeded(equityNow, Instant.now());
            if (state.isPaused()) {
                return false;
            }
            double maxLoss = state.getOpeningEquity() * executeProperties.getRisk().getMaxLossPercent();
            if (state.getRealizedLoss() >= maxLoss) {
                state.setPaused(true);
                persistState();
                return false;
            }
            return true;
        }
    }

    public void addRealizedPnl(double pnl, double equityNow) {
        synchronized (lock) {
            initOrRollIfNeeded(equityNow, Instant.now());
            if (pnl < 0) {
                state.setRealizedLoss(state.getRealizedLoss() + Math.abs(pnl));
            }
            double maxLoss = state.getOpeningEquity() * executeProperties.getRisk().getMaxLossPercent();
            if (state.getRealizedLoss() >= maxLoss) {
                state.setPaused(true);
            }
            persistState();
        }
    }

    public void unblock(double equityNow) {
        synchronized (lock) {
            initOrRollIfNeeded(equityNow, Instant.now());
            state.setPaused(false);
            state.setRealizedLoss(0.0);
            state.setOpeningEquity(equityNow);
            state.setBucketStart(Instant.now());
            persistState();
        }
    }

    public RiskBucketState snapshot(double equityNow) {
        synchronized (lock) {
            initOrRollIfNeeded(equityNow, Instant.now());
            RiskBucketState copy = new RiskBucketState();
            copy.setBucketStart(state.getBucketStart());
            copy.setOpeningEquity(state.getOpeningEquity());
            copy.setRealizedLoss(state.getRealizedLoss());
            copy.setPaused(state.isPaused());
            return copy;
        }
    }

    private void initOrRollIfNeeded(double equityNow, Instant now) {
        if (state == null) {
            repository.findById(1L).ifPresent(db -> {
                RiskBucketState restored = new RiskBucketState();
                restored.setBucketStart(db.getBucketStart());
                restored.setOpeningEquity(db.getOpeningEquity());
                restored.setRealizedLoss(db.getRealizedLoss());
                restored.setPaused(db.isPaused());
                state = restored;
            });
        }
        if (state == null) {
            state = new RiskBucketState();
            state.setBucketStart(now);
            state.setOpeningEquity(equityNow);
            state.setRealizedLoss(0.0);
            state.setPaused(false);
            persistState();
            return;
        }
        long bucketHours = executeProperties.getRisk().getBucketHours();
        Duration elapsed = Duration.between(state.getBucketStart(), now);
        if (elapsed.toHours() >= bucketHours) {
            state.setBucketStart(now);
            state.setOpeningEquity(equityNow);
            state.setRealizedLoss(0.0);
            state.setPaused(false);
            persistState();
        }
    }

    private void persistState() {
        RiskBucketStateEntity entity = new RiskBucketStateEntity();
        entity.setId(1L);
        entity.setBucketStart(state.getBucketStart());
        entity.setOpeningEquity(state.getOpeningEquity());
        entity.setRealizedLoss(state.getRealizedLoss());
        entity.setPaused(state.isPaused());
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }
}
