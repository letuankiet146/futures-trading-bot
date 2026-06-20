package com.trading.simulate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.backtest.BacktestKlineDrillClient;
import com.trading.simulate.backtest.ReplayCandle;
import com.trading.simulate.config.SimulateProperties;
import com.trading.simulate.model.PaperAccountState;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the intra-candle TP/SL drill-down in {@link PaperTradingService}. */
class IntraCandleResolverTest {

    private static final String JOB = "job-1";
    private static final String SYMBOL = "BTCUSDT";

    private SimulateProperties properties;
    private SimPersistenceService persistence;
    private SimAuditService audit;
    private BacktestKlineDrillClient drillClient;
    private PaperTradingService service;

    @BeforeEach
    void setup() {
        properties = new SimulateProperties();
        properties.setSymbol(SYMBOL);
        properties.setLeverage(10);
        properties.setTakerFee(0.0005);
        properties.setInitialBalanceUsdt(1000.0);
        SimulateProperties.Sizing sizing = new SimulateProperties.Sizing();
        sizing.setMode("FIXED");
        sizing.setFixedNotionalUsdt(100.0);
        properties.setSizing(sizing);
        SimulateProperties.Liquidation liq = new SimulateProperties.Liquidation();
        liq.setMarginMode("ISOLATED");
        liq.setIsolatedMarginLossThreshold(0.8);
        properties.setLiquidation(liq);
        // Backtest defaults (tie-break SL, ladder 1h..1m) come from SimulateProperties.Backtest.

        persistence = mock(SimPersistenceService.class);
        audit = mock(SimAuditService.class);
        drillClient = mock(BacktestKlineDrillClient.class);

        service = new PaperTradingService(properties, persistence, audit, drillClient);
        service.init();
    }

    private void openBuy() {
        openPosition("BUY", 100.0, 101.0, 99.0);
    }

    private void openPosition(String side, double entry, double tp, double sl) {
        StrategySignalEvent sig = new StrategySignalEvent();
        sig.setSchemaVersion(3);
        sig.setSymbol(SYMBOL);
        sig.setSide(side);
        sig.setPrice(entry);
        sig.setTakeProfitPrice(tp);
        sig.setStopLossPrice(sl);
        sig.setCorrelationId(JOB);
        sig.setTimestamp("2026-01-01T00:00:00Z");
        service.onStrategySignal(sig);
        assertThat(service.snapshot().getOpenPosition()).isNotNull();
    }

    private ReplayCandle candle(String interval, double open, double high, double low, double close) {
        return new ReplayCandle(interval, 1_700_000_000_000L, 1_700_003_599_999L, open, high, low, close);
    }

    @Test
    void unambiguousTakeProfit_doesNotDrill() {
        openBuy();
        // high reaches TP (>=101), low never reaches SL (<=99): only TP is reachable.
        service.onReplayCandle(SYMBOL, candle("1h", 100.2, 101.5, 100.0, 101.2), JOB);

        PaperAccountState s = service.snapshot();
        assertThat(s.getOpenPosition()).isNull();
        assertThat(s.getStats().getWinCount()).isEqualTo(1);
        assertThat(s.getStats().getLoseCount()).isZero();
        verify(drillClient, never()).fetchFiner(anyString(), any());
    }

    @Test
    void unambiguousStopLoss_sellSide_doesNotDrill() {
        openPosition("SELL", 100.0, 99.0, 101.0);
        // For SELL: SL reachable when high>=101; TP reachable when low<=99. Here only SL is reachable.
        service.onReplayCandle(SYMBOL, candle("1h", 100.0, 101.5, 99.5, 100.8), JOB);

        PaperAccountState s = service.snapshot();
        assertThat(s.getOpenPosition()).isNull();
        assertThat(s.getStats().getLoseCount()).isEqualTo(1);
        assertThat(s.getStats().getWinCount()).isZero();
        verify(drillClient, never()).fetchFiner(anyString(), any());
    }

    @Test
    void ambiguousResolvedAtFinerInterval() {
        openBuy();
        // Parent 1h straddles TP(101) and SL(99): high 102, low 98.
        // The first 30m sub-candle hits only SL, so the trade closes as a loss without deeper drilling.
        when(drillClient.fetchFiner(SYMBOL, candle("1h", 100.0, 102.0, 98.0, 100.0)))
                .thenReturn(List.of(
                        new ReplayCandle("30m", 1_700_000_000_000L, 1_700_001_799_999L, 100.0, 100.5, 98.0, 99.2),
                        new ReplayCandle("30m", 1_700_001_800_000L, 1_700_003_599_999L, 99.2, 100.0, 99.0, 100.0)));

        service.onReplayCandle(SYMBOL, candle("1h", 100.0, 102.0, 98.0, 100.0), JOB);

        PaperAccountState s = service.snapshot();
        assertThat(s.getOpenPosition()).isNull();
        assertThat(s.getStats().getLoseCount()).isEqualTo(1);
        assertThat(s.getStats().getWinCount()).isZero();
        verify(drillClient).fetchFiner(SYMBOL, candle("1h", 100.0, 102.0, 98.0, 100.0));
    }

    @Test
    void ambiguousDownToFinest_appliesSlTieBreak() {
        openBuy();
        // Every level keeps straddling; once the finest interval returns no finer candles, the SL tie-break wins.
        when(drillClient.fetchFiner(anyString(), any())).thenAnswer(invocation -> {
            ReplayCandle parent = invocation.getArgument(1);
            String next = switch (parent.interval()) {
                case "1h" -> "30m";
                case "30m" -> "15m";
                case "15m" -> "3m";
                case "3m" -> "1m";
                default -> null;
            };
            if (next == null) {
                return List.of();
            }
            return List.of(new ReplayCandle(
                    next, parent.openTimeMs(), parent.closeTimeMs(), 100.0, 102.0, 98.0, 100.0));
        });

        service.onReplayCandle(SYMBOL, candle("1h", 100.0, 102.0, 98.0, 100.0), JOB);

        PaperAccountState s = service.snapshot();
        assertThat(s.getOpenPosition()).isNull();
        assertThat(s.getStats().getLoseCount()).isEqualTo(1);
        assertThat(s.getStats().getWinCount()).isZero();
    }
}
