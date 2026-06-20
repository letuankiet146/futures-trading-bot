package com.trading.contracts.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Ordered backtest / paper-replay feed: each OHLC step is a MARK; strategy signals are SIGNAL rows.
 * Single topic preserves mark-then-signal ordering for {@code simulate-service}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulateReplayFeedEvent {
    public static final String TYPE_MARK = "MARK";
    public static final String TYPE_SIGNAL = "SIGNAL";
    public static final String TYPE_CANDLE = "CANDLE";
    /** Control row emitted once at the start of a backtest job to reset the paper account. */
    public static final String TYPE_RESET = "RESET";

    @NotNull
    private Integer schemaVersion;

    /** MARK (price tick) or SIGNAL (strategy signal). */
    @NotBlank
    private String feedType;

    @NotBlank
    private String symbol;

    @NotNull
    private Double price;

    @NotBlank
    private String timestamp;

    /** Required when feedType is SIGNAL. */
    private String side;

    /** Required when feedType is SIGNAL. */
    private String correlationId;

    /** Required when feedType is SIGNAL. */
    private Double takeProfitPrice;

    /** Required when feedType is SIGNAL. */
    private Double stopLossPrice;

    /** Required when feedType is CANDLE. */
    private Double openPrice;

    /** Required when feedType is CANDLE. */
    private Double highPrice;

    /** Required when feedType is CANDLE. */
    private Double lowPrice;

    /** Required when feedType is CANDLE. */
    private Double closePrice;

    /** Required when feedType is CANDLE. Kline open time (epoch ms). */
    private Long openTimeMs;

    /** Required when feedType is CANDLE. Kline close time (epoch ms). */
    private Long closeTimeMs;

    /** Required when feedType is CANDLE. Binance interval code (e.g. 1h, 15m). */
    private String intervalCode;

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getFeedType() {
        return feedType;
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Double getTakeProfitPrice() {
        return takeProfitPrice;
    }

    public void setTakeProfitPrice(Double takeProfitPrice) {
        this.takeProfitPrice = takeProfitPrice;
    }

    public Double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(Double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public Double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
    }

    public Double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }

    public Double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public Double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    public Long getOpenTimeMs() {
        return openTimeMs;
    }

    public void setOpenTimeMs(Long openTimeMs) {
        this.openTimeMs = openTimeMs;
    }

    public Long getCloseTimeMs() {
        return closeTimeMs;
    }

    public void setCloseTimeMs(Long closeTimeMs) {
        this.closeTimeMs = closeTimeMs;
    }

    public String getIntervalCode() {
        return intervalCode;
    }

    public void setIntervalCode(String intervalCode) {
        this.intervalCode = intervalCode;
    }
}
