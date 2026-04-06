package com.trading.execute.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.execute.config.BinanceProperties;
import com.trading.execute.model.SymbolFilters;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class BinanceFuturesClient {
    private final UMFuturesClientImpl client;
    private final ObjectMapper objectMapper;

    public BinanceFuturesClient(BinanceProperties properties, ObjectMapper objectMapper) {
        this.client = new UMFuturesClientImpl(properties.getApiKey(), properties.getSecretKey(), properties.getBaseUrl());
        this.objectMapper = objectMapper;
    }

    public double getUsdtBalance() {
        try {
            String raw = client.account().futuresAccountBalance(new LinkedHashMap<>());
            JsonNode arr = objectMapper.readTree(raw);
            for (JsonNode node : arr) {
                if ("USDT".equalsIgnoreCase(node.path("asset").asText())) {
                    return node.path("balance").asDouble(0.0);
                }
            }
            return 0.0;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch account balance", e);
        }
    }

    public SymbolFilters loadSymbolFilters(String symbol) {
        try {
            String raw = client.market().exchangeInfo();
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode s : root.path("symbols")) {
                if (!symbol.equalsIgnoreCase(s.path("symbol").asText())) {
                    continue;
                }
                SymbolFilters f = new SymbolFilters();
                f.setSymbol(symbol);
                f.setPricePrecision(s.path("pricePrecision").asInt());
                f.setQuantityPrecision(s.path("quantityPrecision").asInt());
                for (JsonNode filter : s.path("filters")) {
                    String type = filter.path("filterType").asText();
                    if ("PRICE_FILTER".equals(type)) {
                        f.setTickSize(filter.path("tickSize").asDouble(0.0));
                    } else if ("LOT_SIZE".equals(type)) {
                        f.setStepSize(filter.path("stepSize").asDouble(0.0));
                        f.setMinQty(filter.path("minQty").asDouble(0.0));
                    } else if ("MIN_NOTIONAL".equals(type) || "NOTIONAL".equals(type)) {
                        f.setMinNotional(filter.path("notional").asDouble(filter.path("minNotional").asDouble(0.0)));
                    }
                }
                return f;
            }
            throw new IllegalStateException("Symbol not found in exchangeInfo: " + symbol);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load exchange filters", e);
        }
    }

    public String placeMarketOrder(String symbol, String side, double quantity, String clientOrderId) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", "MARKET");
        params.put("quantity", quantity);
        params.put("newClientOrderId", clientOrderId);
        return client.account().newOrder(params);
    }

    public void ensureOneWayAndIsolated(String symbol, int leverage) {
        LinkedHashMap<String, Object> positionMode = new LinkedHashMap<>();
        positionMode.put("dualSidePosition", "false");
        client.account().changePositionModeTrade(positionMode);

        LinkedHashMap<String, Object> marginType = new LinkedHashMap<>();
        marginType.put("symbol", symbol);
        marginType.put("marginType", "ISOLATED");
        try {
            client.account().changeMarginType(marginType);
        } catch (Exception ignored) {
            // ignore idempotent already-isolated response
        }

        LinkedHashMap<String, Object> leverageMap = new LinkedHashMap<>();
        leverageMap.put("symbol", symbol);
        leverageMap.put("leverage", leverage);
        client.account().changeInitialLeverage(leverageMap);
    }

    public String placeTakeProfitMarket(String symbol, String side, double quantity, double stopPrice, String clientOrderId) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", "TAKE_PROFIT_MARKET");
        params.put("stopPrice", stopPrice);
        params.put("closePosition", "false");
        params.put("quantity", quantity);
        params.put("reduceOnly", "true");
        params.put("workingType", "MARK_PRICE");
        params.put("newClientOrderId", clientOrderId);
        return client.account().newOrder(params);
    }

    public String placeStopLossMarket(String symbol, String side, double quantity, double stopPrice, String clientOrderId) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", "STOP_MARKET");
        params.put("stopPrice", stopPrice);
        params.put("closePosition", "false");
        params.put("quantity", quantity);
        params.put("reduceOnly", "true");
        params.put("workingType", "MARK_PRICE");
        params.put("newClientOrderId", clientOrderId);
        return client.account().newOrder(params);
    }
}
