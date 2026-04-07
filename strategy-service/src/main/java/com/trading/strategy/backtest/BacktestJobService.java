package com.trading.strategy.backtest;

import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.persistence.BacktestJobRepository;
import com.trading.strategy.persistence.BacktestJobRow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestJobService {

    private final StrategyProperties strategyProperties;
    private final BacktestJobRepository jobRepo;
    private final BacktestJobProcessor processor;

    public BacktestJobService(
            StrategyProperties strategyProperties,
            BacktestJobRepository jobRepo,
            BacktestJobProcessor processor) {
        this.strategyProperties = strategyProperties;
        this.jobRepo = jobRepo;
        this.processor = processor;
    }

    @Transactional
    public UUID createJob(String requestStartRaw, String requestEndRaw) {
        String symbol = strategyProperties.getSymbol();
        String interval = strategyProperties.getInterval();
        String dedupe = BacktestDedupeKeys.build(symbol, interval, requestStartRaw, requestEndRaw);
        Optional<UUID> existing = jobRepo.findActiveIdByDedupeKey(dedupe);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID id = UUID.randomUUID();
        Optional<UUID> inserted =
                jobRepo.insertPendingOrEmptyOnConflict(id, symbol, interval, requestStartRaw, requestEndRaw, dedupe);
        if (inserted.isPresent()) {
            return inserted.get();
        }
        return jobRepo.findActiveIdByDedupeKey(dedupe).orElseThrow(() -> new IllegalStateException("dedupe race"));
    }

    /** Call after {@link #createJob} transaction has committed (e.g. from controller). */
    public void startJobAsync(UUID jobId) {
        processor.processAsync(jobId);
    }

    public Optional<BacktestJobRow> getJob(UUID id) {
        return jobRepo.findById(id);
    }
}
