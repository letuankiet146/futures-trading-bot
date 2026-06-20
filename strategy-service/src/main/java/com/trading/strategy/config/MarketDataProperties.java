package com.trading.strategy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data")
public class MarketDataProperties {
    private String baseRestUrl;
    private String wsBaseUrl;
    private int restLimit;
    private boolean wsEnabled;
    /** Pause inserted before each paged kline REST call to stay under Binance IP rate limits. */
    private long restPageDelayMs = 300;
    /** Number of retries when Binance responds 429 Too Many Requests. */
    private int restMaxRetries = 5;

    public String getBaseRestUrl() {
        return baseRestUrl;
    }

    public void setBaseRestUrl(String baseRestUrl) {
        this.baseRestUrl = baseRestUrl;
    }

    public String getWsBaseUrl() {
        return wsBaseUrl;
    }

    public void setWsBaseUrl(String wsBaseUrl) {
        this.wsBaseUrl = wsBaseUrl;
    }

    public int getRestLimit() {
        return restLimit;
    }

    public void setRestLimit(int restLimit) {
        this.restLimit = restLimit;
    }

    public boolean isWsEnabled() {
        return wsEnabled;
    }

    public void setWsEnabled(boolean wsEnabled) {
        this.wsEnabled = wsEnabled;
    }

    public long getRestPageDelayMs() {
        return restPageDelayMs;
    }

    public void setRestPageDelayMs(long restPageDelayMs) {
        this.restPageDelayMs = restPageDelayMs;
    }

    public int getRestMaxRetries() {
        return restMaxRetries;
    }

    public void setRestMaxRetries(int restMaxRetries) {
        this.restMaxRetries = restMaxRetries;
    }
}
