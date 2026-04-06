package com.trading.strategy.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.strategy.config.MarketDataProperties;
import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.model.Candle;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BinanceWsMarketDataClient {
    private static final Logger log = LoggerFactory.getLogger(BinanceWsMarketDataClient.class);

    private final MarketDataProperties marketDataProperties;
    private final StrategyProperties strategyProperties;
    private final MarketStateStore marketStateStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BinanceWsMarketDataClient(
            MarketDataProperties marketDataProperties,
            StrategyProperties strategyProperties,
            MarketStateStore marketStateStore,
            ObjectMapper objectMapper) {
        this.marketDataProperties = marketDataProperties;
        this.strategyProperties = strategyProperties;
        this.marketStateStore = marketStateStore;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!marketDataProperties.isWsEnabled()) {
            log.info("WS market data disabled by config.");
            return;
        }
        String symbolLower = strategyProperties.getSymbol().toLowerCase();
        String markPath = "/ws/" + symbolLower + "@markPrice";
        String klinePath = "/ws/" + symbolLower + "@kline_" + strategyProperties.getInterval();

        openWs(markPath, new MarkPriceListener());
        openWs(klinePath, new KlineListener());
    }

    private void openWs(String path, WebSocket.Listener listener) {
        String url = marketDataProperties.getWsBaseUrl() + path;
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), listener)
                .thenAccept(ws -> log.info("Connected WS {}", url))
                .exceptionally(ex -> {
                    log.warn("Failed to connect WS {}", url, ex);
                    return null;
                });
    }

    private class MarkPriceListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JsonNode root = objectMapper.readTree(data.toString());
                if (root.has("p")) {
                    marketStateStore.setMarkPrice(root.get("p").asDouble());
                }
            } catch (Exception e) {
                log.warn("Skip malformed markPrice payload: {}", data, e);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }
    }

    private class KlineListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JsonNode root = objectMapper.readTree(data.toString());
                JsonNode k = root.path("k");
                if (!k.isMissingNode()) {
                    Candle candle = new Candle();
                    candle.setOpenTime(k.path("t").asLong());
                    candle.setCloseTime(k.path("T").asLong());
                    candle.setOpen(k.path("o").asDouble());
                    candle.setHigh(k.path("h").asDouble());
                    candle.setLow(k.path("l").asDouble());
                    candle.setClose(k.path("c").asDouble());
                    candle.setClosed(k.path("x").asBoolean(false));
                    marketStateStore.upsertFromWs(candle, strategyProperties.getN());
                }
            } catch (Exception e) {
                log.warn("Skip malformed kline payload: {}", data, e);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }
    }
}
