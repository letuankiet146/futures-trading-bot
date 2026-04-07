package com.trading.strategy.backtest;

import com.trading.strategy.backtest.support.BinanceIntervalMillis;
import com.trading.strategy.market.BinanceKlineRestClient;
import com.trading.strategy.model.Candle;
import com.trading.strategy.persistence.KlineCacheRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KlineBackfillService {

    private static final Logger log = LoggerFactory.getLogger(KlineBackfillService.class);

    private final BinanceKlineRestClient binance;
    private final KlineCacheRepository cache;

    public KlineBackfillService(BinanceKlineRestClient binance, KlineCacheRepository cache) {
        this.binance = binance;
        this.cache = cache;
    }

    /**
     * Open time (ms) of the latest kline that is fully closed relative to {@code nowMs}, using DB first
     * then a small REST tail if needed.
     */
    public long resolveLatestClosedOpenTimeMs(String symbol, String klineInterval, long nowMs) {
        Long db = cache.findLatestClosedOpenTimeMs(symbol, klineInterval, nowMs);
        if (db != null) {
            return db;
        }
        List<Candle> page = binance.fetchKlinesPage(symbol, klineInterval, null, null, 50);
        long best = -1L;
        List<Candle> closed = new ArrayList<>();
        for (Candle c : page) {
            if (c.getCloseTime() <= nowMs) {
                closed.add(c);
                best = Math.max(best, c.getOpenTime());
            }
        }
        if (best < 0) {
            throw new IllegalStateException("No closed kline available from REST for " + symbol + " " + klineInterval);
        }
        cache.insertIgnoreBatch(closed, symbol, klineInterval);
        return best;
    }

    /** Fetch missing segments in [startOpenMs, endOpenMs] and upsert with ON CONFLICT DO NOTHING. */
    public void ensureRangeCached(String symbol, String klineInterval, long startOpenMs, long endOpenMs) {
        if (startOpenMs > endOpenMs) {
            return;
        }
        long step = BinanceIntervalMillis.parse(klineInterval);
        for (int round = 0; round < 64; round++) {
            List<Long> opens = cache.findOpenTimesInRange(symbol, klineInterval, startOpenMs, endOpenMs);
            boolean fetched = false;
            if (opens.isEmpty()) {
                List<Candle> chunk =
                        binance.fetchClosedKlinesRangeBackward(symbol, klineInterval, startOpenMs, endOpenMs);
                cache.insertIgnoreBatch(chunk, symbol, klineInterval);
                log.debug("Backfill full range {} {} candles={}", symbol, klineInterval, chunk.size());
                fetched = true;
            } else {
                long first = opens.get(0);
                if (first > startOpenMs) {
                    List<Candle> chunk =
                            binance.fetchClosedKlinesRangeBackward(symbol, klineInterval, startOpenMs, first - 1);
                    cache.insertIgnoreBatch(chunk, symbol, klineInterval);
                    fetched = true;
                }
                for (int i = 0; i < opens.size() - 1; i++) {
                    long a = opens.get(i);
                    long b = opens.get(i + 1);
                    if (b - a > step) {
                        long gapStart = a + step;
                        long gapEnd = b - step;
                        if (gapStart <= gapEnd) {
                            List<Candle> chunk = binance.fetchClosedKlinesRangeBackward(
                                    symbol, klineInterval, gapStart, gapEnd);
                            cache.insertIgnoreBatch(chunk, symbol, klineInterval);
                            fetched = true;
                        }
                    }
                }
                long last = opens.get(opens.size() - 1);
                if (last < endOpenMs) {
                    long tailStart = last + step;
                    if (tailStart <= endOpenMs) {
                        List<Candle> chunk = binance.fetchClosedKlinesRangeBackward(
                                symbol, klineInterval, tailStart, endOpenMs);
                        cache.insertIgnoreBatch(chunk, symbol, klineInterval);
                        fetched = true;
                    }
                }
            }
            if (!fetched) {
                return;
            }
        }
        throw new IllegalStateException("Backfill did not converge for " + symbol + " " + klineInterval);
    }
}
