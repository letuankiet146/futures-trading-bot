package com.trading.strategy.persistence;

import com.trading.strategy.model.Candle;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class KlineCacheRepository {

    private static final RowMapper<Candle> CANDLE_MAPPER = (rs, rowNum) -> {
        Candle c = new Candle();
        c.setOpenTime(rs.getLong("open_time_ms"));
        c.setCloseTime(rs.getLong("close_time_ms"));
        c.setOpen(rs.getDouble("open_price"));
        c.setHigh(rs.getDouble("high_price"));
        c.setLow(rs.getDouble("low_price"));
        c.setClose(rs.getDouble("close_price"));
        c.setQuoteAssetVolume(rs.getDouble("quote_asset_volume"));
        c.setTakerQuoteAssetVolume(rs.getDouble("taker_quote_asset_volume"));
        c.setClosed(true);
        return c;
    };

    private final JdbcTemplate jdbc;

    public KlineCacheRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Latest fully closed bar: max open_time where close_time <= nowMs. */
    public Long findLatestClosedOpenTimeMs(String symbol, String klineInterval, long nowMs) {
        List<Long> rows = jdbc.query(
                """
                SELECT open_time_ms FROM strategy.backtest_kline
                WHERE symbol = ? AND kline_interval = ? AND close_time_ms <= ?
                ORDER BY open_time_ms DESC
                LIMIT 1
                """,
                (rs, rn) -> rs.getLong(1),
                symbol,
                klineInterval,
                nowMs);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Long> findOpenTimesInRange(String symbol, String klineInterval, long startOpenMs, long endOpenMs) {
        return jdbc.query(
                """
                SELECT open_time_ms FROM strategy.backtest_kline
                WHERE symbol = ? AND kline_interval = ?
                  AND open_time_ms >= ? AND open_time_ms <= ?
                ORDER BY open_time_ms
                """,
                (rs, rn) -> rs.getLong(1),
                symbol,
                klineInterval,
                startOpenMs,
                endOpenMs);
    }

    public List<Candle> findCandlesInRange(String symbol, String klineInterval, long startOpenMs, long endOpenMs) {
        return jdbc.query(
                """
                SELECT open_time_ms, close_time_ms, open_price, high_price, low_price, close_price,
                       quote_asset_volume, taker_quote_asset_volume
                FROM strategy.backtest_kline
                WHERE symbol = ? AND kline_interval = ?
                  AND open_time_ms >= ? AND open_time_ms <= ?
                ORDER BY open_time_ms
                """,
                CANDLE_MAPPER,
                symbol,
                klineInterval,
                startOpenMs,
                endOpenMs);
    }

    /** Insert rows; existing (symbol, interval, open_time) left unchanged. */
    public void insertIgnoreBatch(List<Candle> candles, String symbol, String klineInterval) {
        jdbc.batchUpdate(
                """
                INSERT INTO strategy.backtest_kline
                  (symbol, kline_interval, open_time_ms, open_price, high_price, low_price, close_price,
                   close_time_ms, quote_asset_volume, taker_quote_asset_volume)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (symbol, kline_interval, open_time_ms) DO NOTHING
                """,
                candles,
                candles.size(),
                (ps, c) -> {
                    ps.setString(1, symbol);
                    ps.setString(2, klineInterval);
                    ps.setLong(3, c.getOpenTime());
                    ps.setDouble(4, c.getOpen());
                    ps.setDouble(5, c.getHigh());
                    ps.setDouble(6, c.getLow());
                    ps.setDouble(7, c.getClose());
                    ps.setLong(8, c.getCloseTime());
                    ps.setDouble(9, c.getQuoteAssetVolume());
                    ps.setDouble(10, c.getTakerQuoteAssetVolume());
                });
    }
}
