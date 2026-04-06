package com.trading.strategy.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.strategy.config.MarketDataProperties;
import com.trading.strategy.model.Candle;
import java.util.ArrayList;
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
                candle.setClosed(true);
                result.add(candle);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse kline REST response", e);
        }
        log.info("Loaded {} klines by REST for {} {}", result.size(), symbol, interval);
        return result;
    }
}
