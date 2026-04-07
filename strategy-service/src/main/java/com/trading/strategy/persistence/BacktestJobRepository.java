package com.trading.strategy.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BacktestJobRepository {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    private static final RowMapper<BacktestJobRow> MAPPER = (rs, rn) -> new BacktestJobRow(
            rs.getObject("id", UUID.class),
            rs.getString("status"),
            rs.getString("symbol"),
            rs.getString("kline_interval"),
            rs.getString("request_start_raw"),
            rs.getString("request_end_raw"),
            rs.getString("dedupe_key"),
            rs.getObject("effective_start_ms") != null ? rs.getLong("effective_start_ms") : null,
            rs.getObject("effective_end_ms") != null ? rs.getLong("effective_end_ms") : null,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
            rs.getTimestamp("finished_at") != null ? rs.getTimestamp("finished_at").toInstant() : null,
            rs.getString("error_message"),
            rs.getObject("candles_replayed") != null ? rs.getInt("candles_replayed") : null,
            rs.getObject("sim_balance_usdt") != null ? rs.getDouble("sim_balance_usdt") : null,
            rs.getObject("sim_last_mark_price") != null ? rs.getDouble("sim_last_mark_price") : null,
            rs.getObject("sim_frozen") != null ? rs.getBoolean("sim_frozen") : null,
            rs.getObject("sim_total_trades") != null ? rs.getInt("sim_total_trades") : null,
            rs.getObject("sim_win_count") != null ? rs.getInt("sim_win_count") : null,
            rs.getObject("sim_lose_count") != null ? rs.getInt("sim_lose_count") : null,
            rs.getObject("sim_liquidation_count") != null ? rs.getInt("sim_liquidation_count") : null,
            rs.getObject("sim_total_pnl") != null ? rs.getDouble("sim_total_pnl") : null,
            rs.getObject("sim_total_fees") != null ? rs.getDouble("sim_total_fees") : null,
            rs.getObject("sim_open_position_active") != null ? rs.getBoolean("sim_open_position_active") : null);

    private final JdbcTemplate jdbc;

    public BacktestJobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> findActiveIdByDedupeKey(String dedupeKey) {
        List<UUID> ids = jdbc.query(
                """
                SELECT id FROM strategy.backtest_job
                WHERE dedupe_key = ? AND status IN (?, ?)
                ORDER BY created_at ASC
                LIMIT 1
                """,
                (rs, rn) -> rs.getObject(1, UUID.class),
                dedupeKey,
                STATUS_PENDING,
                STATUS_RUNNING);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public UUID insertPending(
            UUID id,
            String symbol,
            String klineInterval,
            String requestStartRaw,
            String requestEndRaw,
            String dedupeKey) {
        int n = jdbc.update(
                """
                INSERT INTO strategy.backtest_job
                  (id, status, symbol, kline_interval, request_start_raw, request_end_raw, dedupe_key)
                VALUES (?,?,?,?,?,?,?)
                """,
                id,
                STATUS_PENDING,
                symbol,
                klineInterval,
                requestStartRaw,
                requestEndRaw,
                dedupeKey);
        if (n != 1) {
            throw new IllegalStateException("insert strategy.backtest_job expected 1 row");
        }
        return id;
    }

    /** Insert pending job; on partial-unique conflict (duplicate in-flight key), return empty. */
    public Optional<UUID> insertPendingOrEmptyOnConflict(
            UUID id,
            String symbol,
            String klineInterval,
            String requestStartRaw,
            String requestEndRaw,
            String dedupeKey) {
        try {
            insertPending(id, symbol, klineInterval, requestStartRaw, requestEndRaw, dedupeKey);
            return Optional.of(id);
        } catch (DuplicateKeyException e) {
            return Optional.empty();
        }
    }

    /** @return rows updated (1 if job was PENDING) */
    public int markRunning(UUID id) {
        return jdbc.update(
                """
                UPDATE strategy.backtest_job SET status = ?, started_at = ?
                WHERE id = ? AND status = ?
                """,
                STATUS_RUNNING,
                Timestamp.from(Instant.now()),
                id,
                STATUS_PENDING);
    }

    public void updateEffectiveRange(UUID id, long effectiveStartMs, long effectiveEndMs) {
        jdbc.update(
                """
                UPDATE strategy.backtest_job SET effective_start_ms = ?, effective_end_ms = ?
                WHERE id = ?
                """,
                effectiveStartMs,
                effectiveEndMs,
                id);
    }

    public void markSucceeded(UUID id, int candlesReplayed, SimulateBacktestSnapshot snapshot) {
        jdbc.update(
                """
                UPDATE strategy.backtest_job
                SET status = ?, finished_at = ?, candles_replayed = ?, error_message = NULL,
                    sim_balance_usdt = ?, sim_last_mark_price = ?, sim_frozen = ?,
                    sim_total_trades = ?, sim_win_count = ?, sim_lose_count = ?,
                    sim_liquidation_count = ?, sim_total_pnl = ?, sim_total_fees = ?,
                    sim_open_position_active = ?
                WHERE id = ?
                """,
                STATUS_SUCCEEDED,
                Timestamp.from(Instant.now()),
                candlesReplayed,
                snapshot != null ? snapshot.balanceUsdt() : null,
                snapshot != null ? snapshot.lastMarkPrice() : null,
                snapshot != null ? snapshot.frozen() : null,
                snapshot != null ? snapshot.totalTrades() : null,
                snapshot != null ? snapshot.winCount() : null,
                snapshot != null ? snapshot.loseCount() : null,
                snapshot != null ? snapshot.liquidationCount() : null,
                snapshot != null ? snapshot.totalPnl() : null,
                snapshot != null ? snapshot.totalFees() : null,
                snapshot != null ? snapshot.openPositionActive() : null,
                id);
    }

    public void markFailed(UUID id, String message) {
        jdbc.update(
                """
                UPDATE strategy.backtest_job
                SET status = ?, finished_at = ?, error_message = ?
                WHERE id = ?
                """,
                STATUS_FAILED,
                Timestamp.from(Instant.now()),
                message != null && message.length() > 4000 ? message.substring(0, 4000) : message,
                id);
    }

    public Optional<BacktestJobRow> findById(UUID id) {
        List<BacktestJobRow> rows = jdbc.query(
                "SELECT * FROM strategy.backtest_job WHERE id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
