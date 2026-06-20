package com.trading.simulate.backtest;

import com.trading.contracts.util.IntervalMillis;
import com.trading.simulate.config.SimulateProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches finer-interval klines from strategy-service (cache first, then Binance, persisted) so the
 * paper engine can resolve which of TP/SL was touched first inside an ambiguous candle.
 */
@Component
public class BacktestKlineDrillClient {

    private static final Logger log = LoggerFactory.getLogger(BacktestKlineDrillClient.class);

    private final RestClient restClient;
    private final List<String> ladder;

    public BacktestKlineDrillClient(SimulateProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBacktest().getStrategyBaseUrl())
                .build();
        this.ladder = properties.getBacktest().getIntervalLadder();
    }

    /**
     * The largest ladder interval strictly smaller than {@code parentInterval}, or {@code null} when the
     * parent is already at (or below) the finest ladder entry.
     */
    public String nextFiner(String parentInterval) {
        long parentMs = IntervalMillis.parse(parentInterval);
        String best = null;
        long bestMs = -1;
        for (String candidate : ladder) {
            long ms = IntervalMillis.parse(candidate);
            if (ms < parentMs && ms > bestMs) {
                best = candidate;
                bestMs = ms;
            }
        }
        return best;
    }

    /**
     * Sub-candles covering {@code parent}'s window at the next finer ladder interval, ordered by open time.
     * Returns an empty list when there is no finer interval available.
     */
    public List<ReplayCandle> fetchFiner(String symbol, ReplayCandle parent) {
        String finer = nextFiner(parent.interval());
        if (finer == null) {
            return List.of();
        }
        KlineRangeResponse response = restClient
                .post()
                .uri("/api/v1/backtest/klines")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new KlineRangeRequest(symbol, finer, parent.openTimeMs(), parent.closeTimeMs()))
                .retrieve()
                .body(KlineRangeResponse.class);
        if (response == null || response.klines() == null) {
            log.warn("Drill fetch returned empty for symbol={} interval={} window=[{},{}]",
                    symbol, finer, parent.openTimeMs(), parent.closeTimeMs());
            return List.of();
        }
        List<ReplayCandle> result = new ArrayList<>(response.klines().size());
        for (KlineDto k : response.klines()) {
            result.add(new ReplayCandle(
                    finer, k.openTimeMs(), k.closeTimeMs(), k.open(), k.high(), k.low(), k.close()));
        }
        return result;
    }

    public record KlineRangeRequest(String symbol, String interval, long startOpenMs, long endOpenMs) {}

    public record KlineRangeResponse(String symbol, String interval, List<KlineDto> klines) {}

    public record KlineDto(
            long openTimeMs, long closeTimeMs, double open, double high, double low, double close) {}
}
