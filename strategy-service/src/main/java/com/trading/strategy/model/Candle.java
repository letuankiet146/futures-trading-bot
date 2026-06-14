package com.trading.strategy.model;

public class Candle {
    private long openTime;
    private long closeTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private boolean closed;

    // Quote asset volume
    private double quoteAssetVolume;

    // Taker buy quote asset volume
    private double takerQuoteAssetVolume;

    public long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(long openTime) {
        this.openTime = openTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public double getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public void setQuoteAssetVolume(double quoteAssetVolume) {
        this.quoteAssetVolume = quoteAssetVolume;
    }

    public double getTakerQuoteAssetVolume() {
        return takerQuoteAssetVolume;
    }

    public void setTakerQuoteAssetVolume(double takerQuoteAssetVolume) {
        this.takerQuoteAssetVolume = takerQuoteAssetVolume;
    }
}
