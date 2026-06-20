package com.trading.execute.service;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.execute.binance.BinanceFuturesClient;
import com.trading.execute.config.ExecuteProperties;
import com.trading.execute.model.SymbolFilters;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LiveExecutionService {
    private static final Logger log = LoggerFactory.getLogger(LiveExecutionService.class);

    private final BinanceFuturesClient binanceClient;
    private final ExchangeMetadataService metadataService;
    private final RiskSizingService sizingService;
    private final RiskBucketService riskBucketService;
    private final BalanceService balanceService;
    private final BalanceReconciliationService balanceReconciliationService;
    private final ExecutePersistenceService persistenceService;
    private final ExecuteAuditService auditService;
    private final ExecuteProperties executeProperties;

    public LiveExecutionService(
            BinanceFuturesClient binanceClient,
            ExchangeMetadataService metadataService,
            RiskSizingService sizingService,
            RiskBucketService riskBucketService,
            BalanceService balanceService,
            BalanceReconciliationService balanceReconciliationService,
            ExecutePersistenceService persistenceService,
            ExecuteAuditService auditService,
            ExecuteProperties executeProperties) {
        this.binanceClient = binanceClient;
        this.metadataService = metadataService;
        this.sizingService = sizingService;
        this.riskBucketService = riskBucketService;
        this.balanceService = balanceService;
        this.balanceReconciliationService = balanceReconciliationService;
        this.persistenceService = persistenceService;
        this.auditService = auditService;
        this.executeProperties = executeProperties;
    }

    public void process(StrategySignalEvent event) {
        if (!"LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            return;
        }
        if (balanceReconciliationService.isSafeModeActive()) {
            log.warn("Execution blocked by reconciliation safe mode correlationId={}", event.getCorrelationId());
            auditService.log("RECONCILE_BLOCK", event.getCorrelationId(), "Blocked by reconciliation safe mode");
            return;
        }
        binanceClient.ensureOneWayAndIsolated(event.getSymbol(), executeProperties.getLeverage());
        double balance = balanceService.currentUsdtBalance();
        if (!riskBucketService.isAllowed(balance)) {
            log.warn("Risk paused. skip execution for correlationId={}", event.getCorrelationId());
            auditService.log("RISK_PAUSED", event.getCorrelationId(), "Execution skipped due to risk pause");
            return;
        }

        SymbolFilters filters = metadataService.getSymbolFilters();
        double quantity = sizingService.calculateQuantity(balance, event.getPrice(), filters);

        String entryClientId = "ent-" + shortId();
        String entryResult = binanceClient.placeMarketOrder(
                event.getSymbol(),
                event.getSide(),
                quantity,
                entryClientId);
        log.info("Submitted entry order correlationId={} clientOrderId={} result={}",
                event.getCorrelationId(), entryClientId, entryResult);
        persistenceService.saveEntryOrder(event.getSymbol(), event.getSide(), quantity, entryClientId, event.getCorrelationId());
        persistenceService.openPosition(event.getSymbol(), event.getSide(), quantity, event.getPrice());
        persistenceService.saveFill(event.getSymbol(), event.getSide(), quantity, event.getPrice(), "ENTRY");
        auditService.log("ENTRY_SUBMITTED", event.getCorrelationId(), "Entry order submitted: " + entryClientId);

        if (event.getTakeProfitPrice() == null || event.getStopLossPrice() == null) {
            log.warn("Execution skipped: strategy signal missing TP/SL correlationId={}", event.getCorrelationId());
            auditService.log("BRACKET_REJECTED", event.getCorrelationId(), "Missing strategy TP/SL on signal");
            return;
        }

        String closingSide = "BUY".equalsIgnoreCase(event.getSide()) ? "SELL" : "BUY";
        double tpPrice = sizingService.normalizeStopPrice(event.getTakeProfitPrice(), filters);
        double slPrice = sizingService.normalizeStopPrice(event.getStopLossPrice(), filters);

        String tpId = "tp-" + shortId();
        String slId = "sl-" + shortId();
        String tpResult = binanceClient.placeTakeProfitMarket(event.getSymbol(), closingSide, quantity, tpPrice, tpId);
        String slResult = binanceClient.placeStopLossMarket(event.getSymbol(), closingSide, quantity, slPrice, slId);
        persistenceService.saveBracketOrder(event.getSymbol(), closingSide, quantity, tpId, "TAKE_PROFIT_MARKET", event.getCorrelationId());
        persistenceService.saveBracketOrder(event.getSymbol(), closingSide, quantity, slId, "STOP_MARKET", event.getCorrelationId());
        auditService.log("BRACKET_SUBMITTED", event.getCorrelationId(), "TP/SL submitted");
        log.info("Submitted bracket correlationId={} tp={} sl={} tpResult={} slResult={}",
                event.getCorrelationId(), tpPrice, slPrice, tpResult, slResult);
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
