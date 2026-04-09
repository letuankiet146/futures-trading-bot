package com.trading.simulate.persistence.strategy;

import com.trading.simulate.model.JobTimelineCandle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Read-only access to strategy-service backtest tables (same Postgres DB) for chart OHLC.
 */
@Repository
public class StrategyBacktestChartDataRepository {

    private static final RowMapper<JobTimelineCandle> CANDLE_ROW = (rs, rn) -> new JobTimelineCandle(
            Instant.ofEpochMilli(rs.getLong("open_time_ms")),
            rs.getDouble("open_price"),
            rs.getDouble("high_price"),
            rs.getDouble("low_price"),
            rs.getDouble("close_price"),
            0.0);

    private final JdbcTemplate jdbc;

    public StrategyBacktestChartDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<BacktestJobKlineRange> findJobKlineRange(UUID jobId) {
        try {
            List<BacktestJobKlineRange> rows = jdbc.query(
                    """
                    SELECT symbol, kline_interval, effective_start_ms, effective_end_ms
                    FROM strategy.backtest_job
                    WHERE id = ?
                    """,
                    (rs, rn) -> new BacktestJobKlineRange(
                            rs.getString("symbol"),
                            rs.getString("kline_interval"),
                            rs.getObject("effective_start_ms") != null ? rs.getLong("effective_start_ms") : null,
                            rs.getObject("effective_end_ms") != null ? rs.getLong("effective_end_ms") : null),
                    jobId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public List<JobTimelineCandle> findKlinesInRange(
            String symbol, String klineInterval, long startOpenMsInclusive, long endOpenMsInclusive) {
        try {
            return jdbc.query(
                    """
                    SELECT open_time_ms, open_price, high_price, low_price, close_price
                    FROM strategy.backtest_kline
                    WHERE symbol = ? AND kline_interval = ?
                      AND open_time_ms >= ? AND open_time_ms <= ?
                    ORDER BY open_time_ms
                    """,
                    CANDLE_ROW,
                    symbol,
                    klineInterval,
                    startOpenMsInclusive,
                    endOpenMsInclusive);
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    public record BacktestJobKlineRange(
            String symbol, String klineInterval, Long effectiveStartMs, Long effectiveEndMs) {

        public boolean isComplete() {
            return symbol != null
                    && klineInterval != null
                    && effectiveStartMs != null
                    && effectiveEndMs != null
                    && effectiveEndMs >= effectiveStartMs;
        }
    }
}
