package com.trading.strategy.market;

import com.trading.strategy.config.MarketDataProperties;
import com.trading.strategy.config.StrategyProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarketBootstrapLoader {
    private static final Logger log = LoggerFactory.getLogger(MarketBootstrapLoader.class);

    private final BinanceKlineRestClient restClient;
    private final StrategyProperties strategyProperties;
    private final MarketDataProperties marketDataProperties;
    private final MarketStateStore marketStateStore;

    public MarketBootstrapLoader(
            BinanceKlineRestClient restClient,
            StrategyProperties strategyProperties,
            MarketDataProperties marketDataProperties,
            MarketStateStore marketStateStore) {
        this.restClient = restClient;
        this.strategyProperties = strategyProperties;
        this.marketDataProperties = marketDataProperties;
        this.marketStateStore = marketStateStore;
    }

    @PostConstruct
    public void loadInitialCandles() {
        int limit = Math.min(strategyProperties.getN(), marketDataProperties.getRestLimit());
        if (limit <= 0) {
            log.warn("Skip bootstrap because kline limit is {}", limit);
            return;
        }
        marketStateStore.replaceClosedCandles(
                restClient.loadClosedKlines(strategyProperties.getSymbol(), strategyProperties.getInterval(), limit),
                strategyProperties.getN());
    }
}
