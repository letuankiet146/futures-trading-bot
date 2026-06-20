package com.trading.simulate.model;

import java.time.Instant;

/**
 * A position still open at the end of a job's replay. Surfaced so the UI/API can list it separately from
 * closed round-trip trades, marked to the last candle close for unrealized PnL.
 */
public record JobTimelineOpenPosition(
        String side,
        double entryPrice,
        double quantity,
        Double takeProfitPrice,
        Double stopLossPrice,
        double markPrice,
        double unrealizedPnl,
        Instant entryTime) {
}
