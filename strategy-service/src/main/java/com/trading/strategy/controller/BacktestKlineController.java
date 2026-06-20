package com.trading.strategy.controller;

import com.trading.strategy.backtest.KlineBackfillService;
import com.trading.strategy.model.Candle;
import com.trading.strategy.persistence.KlineCacheRepository;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Internal endpoint used by simulate-service to drill down into a finer interval for a candle window.
 * Ensures the requested range is cached (DB first, then Binance, persisted for reuse) and returns it.
 */
@RestController
@RequestMapping("/api/v1/backtest")
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestKlineController {

    /** Intervals the drill-down ladder may request. */
    private static final Set<String> ALLOWED_INTERVALS = Set.of("1h", "30m", "15m", "3m", "1m");

    private final KlineBackfillService backfill;
    private final KlineCacheRepository klineCache;

    public BacktestKlineController(KlineBackfillService backfill, KlineCacheRepository klineCache) {
        this.backfill = backfill;
        this.klineCache = klineCache;
    }

    @PostMapping("/klines")
    public KlineRangeResponse klines(@RequestBody KlineRangeRequest request) {
        if (request == null
                || request.symbol() == null
                || request.symbol().isBlank()
                || request.interval() == null
                || request.interval().isBlank()
                || request.startOpenMs() == null
                || request.endOpenMs() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "symbol, interval, startOpenMs and endOpenMs are required");
        }
        String interval = request.interval().trim();
        if (!ALLOWED_INTERVALS.contains(interval)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "interval must be one of " + ALLOWED_INTERVALS);
        }
        if (request.startOpenMs() > request.endOpenMs()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startOpenMs must be <= endOpenMs");
        }
        String symbol = request.symbol().trim();
        backfill.ensureRangeCached(symbol, interval, request.startOpenMs(), request.endOpenMs());
        List<Candle> candles =
                klineCache.findCandlesInRange(symbol, interval, request.startOpenMs(), request.endOpenMs());
        List<KlineDto> rows = candles.stream()
                .map(c -> new KlineDto(
                        c.getOpenTime(), c.getCloseTime(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose()))
                .toList();
        return new KlineRangeResponse(symbol, interval, rows);
    }

    public record KlineRangeRequest(String symbol, String interval, Long startOpenMs, Long endOpenMs) {}

    public record KlineRangeResponse(String symbol, String interval, List<KlineDto> klines) {}

    public record KlineDto(
            long openTimeMs, long closeTimeMs, double open, double high, double low, double close) {}
}
