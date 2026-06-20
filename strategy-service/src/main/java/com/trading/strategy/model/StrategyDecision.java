package com.trading.strategy.model;

public record StrategyDecision(
        boolean shouldSignal,
        String side,
        double avgTop,
        double avgBottom,
        /** Last closed candle close; used as signal execution reference when {@link #shouldSignal} is true. */
        double signalPrice,
        Double takeProfitPrice,
        Double stopLossPrice) {
}
