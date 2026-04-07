package com.trading.strategy.persistence;

public record SimulateBacktestSnapshot(
        Double balanceUsdt,
        Double lastMarkPrice,
        Boolean frozen,
        Integer totalTrades,
        Integer winCount,
        Integer loseCount,
        Integer liquidationCount,
        Double totalPnl,
        Double totalFees,
        Boolean openPositionActive) {}
