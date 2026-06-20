package com.trading.strategy.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.strategy.config.MarketDataProperties;
import com.trading.strategy.model.Candle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class BinanceKlineRestClient {
    private static final Logger log = LoggerFactory.getLogger(BinanceKlineRestClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MarketDataProperties marketDataProperties;

    public BinanceKlineRestClient(MarketDataProperties marketDataProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(marketDataProperties.getBaseRestUrl()).build();
        this.objectMapper = objectMapper;
        this.marketDataProperties = marketDataProperties;
    }

    public List<Candle> loadClosedKlines(String symbol, String interval, int limit) {
        String payload = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(String.class);

        List<Candle> result = parseKlines(payload);
        log.info("Loaded {} klines by REST for {} {}", result.size(), symbol, interval);
        return result;
    }

    /**
     * One page of klines (ascending by open time). Binance caps {@code limit} at 1500.
     * Omit {@code startTime} / {@code endTime} when null.
     */
    public List<Candle> fetchKlinesPage(
            String symbol, String interval, Long startTimeMs, Long endTimeMs, int limit) {
        int capped = Math.min(Math.max(limit, 1), 1500);
        String payload = getWithThrottleAndRetry(() -> restClient
                .get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/fapi/v1/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("limit", capped);
                    if (startTimeMs != null) {
                        b.queryParam("startTime", startTimeMs);
                    }
                    if (endTimeMs != null) {
                        b.queryParam("endTime", endTimeMs);
                    }
                    return b.build();
                })
                .retrieve()
                .body(String.class));

        return parseKlines(payload);
    }

    /**
     * Throttles each paged request and retries on 429 Too Many Requests, honoring {@code Retry-After}
     * when present and falling back to exponential backoff otherwise.
     */
    private String getWithThrottleAndRetry(java.util.function.Supplier<String> call) {
        sleepMs(marketDataProperties.getRestPageDelayMs());
        int maxRetries = Math.max(0, marketDataProperties.getRestMaxRetries());
        HttpClientErrorException lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return call.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                lastError = e;
                long waitMs = resolveBackoffMs(e, attempt);
                log.warn(
                        "Binance 429 Too Many Requests (attempt {}/{}), backing off {} ms",
                        attempt + 1,
                        maxRetries + 1,
                        waitMs);
                sleepMs(waitMs);
            }
        }
        throw new IllegalStateException("Binance kline REST rate limited after retries", lastError);
    }

    private long resolveBackoffMs(HttpClientErrorException e, int attempt) {
        HttpHeaders headers = e.getResponseHeaders();
        if (headers != null) {
            String retryAfter = headers.getFirst("Retry-After");
            if (retryAfter != null) {
                try {
                    return Math.max(1000L, Long.parseLong(retryAfter.trim()) * 1000L);
                } catch (NumberFormatException ignored) {
                    // fall through to exponential backoff
                }
            }
        }
        long base = Math.max(1000L, marketDataProperties.getRestPageDelayMs());
        return base * (1L << Math.min(attempt, 5));
    }

    private void sleepMs(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while throttling Binance kline REST calls", e);
        }
    }

    private List<Candle> parseKlines(String payload) {
        List<Candle> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(payload);
            for (JsonNode row : root) {
                Candle candle = new Candle();
                candle.setOpenTime(row.get(0).asLong());
                candle.setOpen(row.get(1).asDouble());
                candle.setHigh(row.get(2).asDouble());
                candle.setLow(row.get(3).asDouble());
                candle.setClose(row.get(4).asDouble());
                candle.setCloseTime(row.get(6).asLong());
                candle.setQuoteAssetVolume(row.get(7).asDouble());
                candle.setTakerQuoteAssetVolume(row.get(10).asDouble());
                candle.setClosed(true);
                result.add(candle);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse kline REST response", e);
        }
        return result;
    }

    /**
     * Walks backward from {@code endOpenMs} until coverage reaches {@code startOpenMs}, then returns
     * candles in [startOpenMs, endOpenMs] by open time (ascending).
     */
    public List<Candle> fetchClosedKlinesRangeBackward(String symbol, String interval, long startOpenMs, long endOpenMs) {
        if (startOpenMs > endOpenMs) {
            return Collections.emptyList();
        }
        List<Candle> collected = new ArrayList<>();
        Long cursorEnd = endOpenMs;
        int guard = 0;
        final int maxPages = 10_000;
        while (guard++ < maxPages) {
            List<Candle> page = fetchKlinesPage(symbol, interval, null, cursorEnd, 1500);
            if (page.isEmpty()) {
                break;
            }
            long minOpen = Long.MAX_VALUE;
            for (Candle c : page) {
                minOpen = Math.min(minOpen, c.getOpenTime());
                if (c.getOpenTime() >= startOpenMs && c.getOpenTime() <= endOpenMs) {
                    collected.add(c);
                }
            }
            if (minOpen <= startOpenMs) {
                break;
            }
            cursorEnd = minOpen - 1;
        }
        collected.sort((a, b) -> Long.compare(a.getOpenTime(), b.getOpenTime()));
        return collected;
    }
}
