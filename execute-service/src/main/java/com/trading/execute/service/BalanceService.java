package com.trading.execute.service;

import com.trading.execute.binance.BinanceFuturesClient;
import com.trading.execute.config.ExecuteProperties;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
    private final BinanceFuturesClient binanceFuturesClient;
    private final ExecuteProperties executeProperties;
    private final AtomicReference<Double> localCachedBalance = new AtomicReference<>(1000.0);

    public BalanceService(BinanceFuturesClient binanceFuturesClient, ExecuteProperties executeProperties) {
        this.binanceFuturesClient = binanceFuturesClient;
        this.executeProperties = executeProperties;
    }

    @PostConstruct
    public void init() {
        if ("LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            try {
                localCachedBalance.set(binanceFuturesClient.getUsdtBalance());
            } catch (Exception ignored) {
                // keep fallback value until reconciliation succeeds
            }
        }
    }

    public double currentUsdtBalance() {
        if (!"LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            // M4 scope is live path; use a stable non-zero fallback in non-live mode.
            return 1000.0;
        }
        Double v = localCachedBalance.get();
        return v == null ? 0.0 : v;
    }

    public double fetchExchangeBalance() {
        if (!"LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            return currentUsdtBalance();
        }
        return binanceFuturesClient.getUsdtBalance();
    }

    public void overwriteLocalBalance(double value) {
        localCachedBalance.set(value);
    }
}
