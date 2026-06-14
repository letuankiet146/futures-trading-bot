package com.trading.strategy.engine;

import com.trading.strategy.config.StrategyProperties;
import com.trading.strategy.config.StrategyProperties.VortexScalping;
import com.trading.strategy.indicator.TechnicalIndicators;
import com.trading.strategy.model.Candle;
import com.trading.strategy.model.StrategyDecision;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.strategy.type", havingValue = "vortex-scalping")
public class VortexScalpingStrategy implements TradingStrategy {
    private final StrategyProperties strategyProperties;

    public VortexScalpingStrategy(StrategyProperties strategyProperties) {
        this.strategyProperties = strategyProperties;
    }

    @Override
    public StrategyDecision evaluate(List<Candle> closedCandles, double markPrice) {
        VortexScalping cfg = strategyProperties.getVortex();
        int minBars = Math.max(cfg.getEmaPeriod(), cfg.getRsiPeriod() + 1) + cfg.getVolumeLookback();
        if (closedCandles.size() < minBars) {
            return noSignal();
        }

        List<Double> closes = new ArrayList<>(closedCandles.size());
        List<Double> takerBuyVolumes = new ArrayList<>(closedCandles.size());
        List<Double> takerBuyRatios = new ArrayList<>(closedCandles.size());
        for (Candle candle : closedCandles) {
            closes.add(candle.getClose());
            takerBuyVolumes.add(candle.getTakerQuoteAssetVolume());
            double quoteVol = candle.getQuoteAssetVolume();
            takerBuyRatios.add(quoteVol > 0 ? candle.getTakerQuoteAssetVolume() / quoteVol : 0.0);
        }

        int lastIdx = closedCandles.size();
        double price = closedCandles.get(lastIdx - 1).getClose();
        double ema = TechnicalIndicators.ema(closes, cfg.getEmaPeriod());
        double rsi = TechnicalIndicators.rsi(closes, cfg.getRsiPeriod());
        if (Double.isNaN(ema) || Double.isNaN(rsi)) {
            return noSignal();
        }

        double currentTakerBuy = takerBuyVolumes.get(lastIdx - 1);
        double avgTakerBuy = TechnicalIndicators.average(takerBuyVolumes, cfg.getVolumeLookback(), lastIdx - 1);
        double currentBuyRatio = takerBuyRatios.get(lastIdx - 1);
        double avgBuyRatio = TechnicalIndicators.average(takerBuyRatios, cfg.getVolumeLookback(), lastIdx - 1);

        if (Double.isNaN(avgTakerBuy) || Double.isNaN(avgBuyRatio)) {
            return noSignal();
        }

        Candle last = closedCandles.get(lastIdx - 1);
        double quoteVol = last.getQuoteAssetVolume();
        double takerSellVol = quoteVol - currentTakerBuy;

        boolean longEntry = price > ema
                && rsi < cfg.getRsiOversold()
                && currentTakerBuy > cfg.getTakerBuyVolumeMultiplier() * avgTakerBuy
                ;

        boolean shortEntry = price < ema
                && rsi > cfg.getRsiOverbought()
                && takerSellVol > currentTakerBuy
                && currentBuyRatio < avgBuyRatio * cfg.getTakerBuyRatioDropFactor()
                ;

        if (longEntry) {
            return new StrategyDecision(true, "BUY", ema, rsi, price);
        }
        if (shortEntry) {
            return new StrategyDecision(true, "SELL", ema, rsi, price);
        }
        return new StrategyDecision(false, "NONE", ema, rsi, 0.0);
    }

    private StrategyDecision noSignal() {
        return new StrategyDecision(false, "NONE", 0.0, 0.0, 0.0);
    }
}
