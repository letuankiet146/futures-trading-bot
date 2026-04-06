package com.trading.execute.controller;

import com.trading.execute.config.ExecuteProperties;
import com.trading.execute.model.RiskBucketState;
import com.trading.execute.service.BalanceService;
import com.trading.execute.service.ExecuteAuditService;
import com.trading.execute.service.RiskBucketService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {
    private final ExecuteProperties executeProperties;
    private final RiskBucketService riskBucketService;
    private final BalanceService balanceService;
    private final ExecuteAuditService auditService;

    public RiskController(
            ExecuteProperties executeProperties,
            RiskBucketService riskBucketService,
            BalanceService balanceService,
            ExecuteAuditService auditService) {
        this.executeProperties = executeProperties;
        this.riskBucketService = riskBucketService;
        this.balanceService = balanceService;
        this.auditService = auditService;
    }

    @PostMapping("/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@RequestHeader("X-Admin-Token") String token) {
        validateToken(token);
        riskBucketService.unblock(balanceService.currentUsdtBalance());
        auditService.log("RISK_UNBLOCK", null, "Manual unblock invoked");
    }

    @PostMapping("/realized-pnl")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordRealizedPnl(
            @RequestHeader("X-Admin-Token") String token,
            @RequestBody RealizedPnlRequest request) {
        validateToken(token);
        riskBucketService.addRealizedPnl(request.realizedPnl(), balanceService.currentUsdtBalance());
        auditService.log("RISK_REALIZED_PNL", null, "Realized PnL update: " + request.realizedPnl());
    }

    @GetMapping("/state")
    public RiskBucketState state() {
        return riskBucketService.snapshot(balanceService.currentUsdtBalance());
    }

    private void validateToken(String token) {
        if (!executeProperties.getAuth().getAdminToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token");
        }
    }

    public record RealizedPnlRequest(@NotNull Double realizedPnl) {
    }
}
