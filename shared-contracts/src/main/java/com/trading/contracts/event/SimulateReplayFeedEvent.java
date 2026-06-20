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
}
