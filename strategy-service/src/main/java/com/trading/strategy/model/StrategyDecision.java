package com.trading.strategy.model;

public record StrategyDecision(
        boolean shouldSignal,
        String side,
        double avgTop,
        double avgBottom) {
}
