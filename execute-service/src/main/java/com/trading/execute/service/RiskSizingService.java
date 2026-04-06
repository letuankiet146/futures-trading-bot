package com.trading.execute.service;

import com.trading.execute.config.ExecuteProperties;
import com.trading.execute.model.SymbolFilters;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class RiskSizingService {
    private final ExecuteProperties executeProperties;

    public RiskSizingService(ExecuteProperties executeProperties) {
        this.executeProperties = executeProperties;
    }

    public double calculateQuantity(double equityUsdt, double markPrice, SymbolFilters filters) {
        ExecuteProperties.Sizing sizing = executeProperties.getSizing();
        double targetNotional;
        if ("FIXED".equalsIgnoreCase(sizing.getMode())) {
            targetNotional = sizing.getFixedNotionalUsdt();
        } else {
            double rawPercent = Math.min(sizing.getAccountPercent(), sizing.getMaxAccountPercent());
            targetNotional = equityUsdt * rawPercent;
        }

        double quantityRaw = targetNotional / markPrice;
        double quantityStepped = floorToStep(quantityRaw, filters.getStepSize());
        quantityStepped = roundToPrecision(quantityStepped, filters.getQuantityPrecision());
        if (quantityStepped < filters.getMinQty()) {
            throw new IllegalArgumentException("Quantity below minQty after rounding");
        }
        double notional = quantityStepped * markPrice;
        if (notional < filters.getMinNotional()) {
            throw new IllegalArgumentException("Notional below minNotional after rounding");
        }
        return quantityStepped;
    }

    public double normalizeStopPrice(double rawStopPrice, SymbolFilters filters) {
        double stepped = floorToStep(rawStopPrice, filters.getTickSize());
        return roundToPrecision(stepped, filters.getPricePrecision());
    }

    private double floorToStep(double value, double step) {
        if (step <= 0) {
            return value;
        }
        return Math.floor(value / step) * step;
    }

    private double roundToPrecision(double value, int precision) {
        return BigDecimal.valueOf(value).setScale(Math.max(0, precision), RoundingMode.DOWN).doubleValue();
    }
}
