package com.trading.strategy.persistence;

import java.time.Instant;

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
        Boolean openPositionActive,
        Double unrealizedPnl,
        OpenPosition openPosition) {

    /** Details of a position still open at the end of the replay (marked to the last candle close). */
    public record OpenPosition(
            String side,
            Double entryPrice,
            Double quantity,
            Double takeProfitPrice,
            Double stopLossPrice,
            Double markPrice,
            Double unrealizedPnl,
            Instant entryTime) {}
}
