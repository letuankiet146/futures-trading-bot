package com.trading.execute.service;

import com.trading.execute.binance.BinanceFuturesClient;
import com.trading.execute.config.ExecuteProperties;
import com.trading.execute.model.SymbolFilters;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class ExchangeMetadataService {
    private final BinanceFuturesClient binanceClient;
    private final ExecuteProperties executeProperties;
    private volatile SymbolFilters symbolFilters;

    public ExchangeMetadataService(BinanceFuturesClient binanceClient, ExecuteProperties executeProperties) {
        this.binanceClient = binanceClient;
        this.executeProperties = executeProperties;
    }

    @PostConstruct
    public void init() {
        if (!"LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            return;
        }
        this.symbolFilters = binanceClient.loadSymbolFilters(executeProperties.getSymbol());
    }

    public SymbolFilters getSymbolFilters() {
        return symbolFilters;
    }
}
