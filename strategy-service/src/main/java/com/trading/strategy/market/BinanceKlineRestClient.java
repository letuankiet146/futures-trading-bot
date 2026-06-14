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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceKlineRestClient {
    private static final Logger log = LoggerFactory.getLogger(BinanceKlineRestClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BinanceKlineRestClient(MarketDataProperties marketDataProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(marketDataProperties.getBaseRestUrl()).build();
        this.objectMapper = objectMapper;
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
        String payload = restClient
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
                .body(String.class);

        return parseKlines(payload);
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
