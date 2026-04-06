package com.trading.strategy.controller;

import com.trading.contracts.event.StrategySignalEvent;
import com.trading.strategy.kafka.StrategySignalPublisher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/signals")
public class SignalController {
    private final StrategySignalPublisher publisher;

    public SignalController(StrategySignalPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/test")
    public StrategySignalEvent publish(@RequestBody TestSignalRequest request) {
        return publisher.publishTestSignal(request.symbol(), request.side(), request.price());
    }

    public record TestSignalRequest(
            @NotBlank String symbol,
            @NotBlank String side,
            @NotNull Double price) {
    }
}
