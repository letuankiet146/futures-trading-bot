package com.trading.simulate.model;

public record JobTimelineSummary(
        int wins,
        int losses,
        int liquidations,
        int totalTrades,
        double totalPnl,
        double totalFees) {
}
