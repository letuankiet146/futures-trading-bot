package com.trading.strategy.engine;

import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.config.StrategyProperties.Exit;

/** Resolves percent-based TP/SL distance rates from strategy exit config. */
public final class ExitDistanceResolver {
    private ExitDistanceResolver() {
    }

    public static double tpDistanceRate(StrategyProperties properties) {
        Exit exit = properties.getExit();
        if (exit.getTakeProfitPercent() > 0) {
            return exit.getTakeProfitPercent();
        }
        double feeDistance = properties.getFeeGateMultiplier() * properties.getTakerFee();
        return feeDistance * exit.getTpMultiplier();
    }

    public static double slDistanceRate(StrategyProperties properties) {
        Exit exit = properties.getExit();
        if (exit.getStopLossPercent() > 0) {
            return exit.getStopLossPercent();
        }
        double feeDistance = properties.getFeeGateMultiplier() * properties.getTakerFee();
        return feeDistance * exit.getSlMultiplier();
    }
}
