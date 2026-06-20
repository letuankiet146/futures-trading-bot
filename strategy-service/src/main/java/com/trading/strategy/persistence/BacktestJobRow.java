package com.trading.strategy.persistence;

import java.time.Instant;
import java.util.UUID;

public record BacktestJobRow(
        UUID id,
        String status,
        String symbol,
        String klineInterval,
        String requestStartRaw,
        String requestEndRaw,
        Double requestTpPercent,
        Double requestSlPercent,
        String dedupeKey,
        Long effectiveStartMs,
        Long effectiveEndMs,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Integer candlesReplayed,
        Double simBalanceUsdt,
        Double simLastMarkPrice,
        Boolean simFrozen,
        Integer simTotalTrades,
        Integer simWinCount,
        Integer simLoseCount,
        Integer simLiquidationCount,
        Double simTotalPnl,
        Double simTotalFees,
        Boolean simOpenPositionActive) {}
