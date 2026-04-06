package com.trading.execute.service;

import com.trading.execute.config.ExecuteProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BalanceReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(BalanceReconciliationService.class);

    private final ExecuteProperties executeProperties;
    private final BalanceService balanceService;
    private final ExecuteAuditService auditService;
    private final AtomicBoolean safeMode = new AtomicBoolean(false);

    public BalanceReconciliationService(
            ExecuteProperties executeProperties,
            BalanceService balanceService,
            ExecuteAuditService auditService) {
        this.executeProperties = executeProperties;
        this.balanceService = balanceService;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${app.execute.reconciliation.sync-ms}")
    public void reconcile() {
        if (!"LIVE".equalsIgnoreCase(executeProperties.getMode())) {
            return;
        }
        try {
            double local = balanceService.currentUsdtBalance();
            double exchange = balanceService.fetchExchangeBalance();
            double drift = Math.abs(exchange - local);
            if (drift > executeProperties.getReconciliation().getDriftUsdt()) {
                safeMode.set(true);
                auditService.log("RECONCILE_DRIFT", null, "Drift detected local=" + local + ", exchange=" + exchange);
            }
            balanceService.overwriteLocalBalance(exchange);
            if (safeMode.get()) {
                auditService.log("RECONCILE_RECOVERED", null, "Local balance overwritten from exchange");
            }
            safeMode.set(false);
        } catch (Exception ex) {
            safeMode.set(true);
            auditService.log("RECONCILE_ERROR", null, "Reconciliation failed: " + ex.getMessage());
            log.warn("Reconciliation failure: entering safe mode", ex);
        }
    }

    public boolean isSafeModeActive() {
        return safeMode.get();
    }
}
