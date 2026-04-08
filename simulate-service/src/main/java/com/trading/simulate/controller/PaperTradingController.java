package com.trading.simulate.controller;

import com.trading.simulate.config.SimulateProperties;
import com.trading.simulate.model.JobTimeline;
import com.trading.simulate.model.PaperAccountState;
import com.trading.simulate.service.PaperTradingService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/v1/simulate")
public class PaperTradingController {
    private final PaperTradingService paperTradingService;
    private final SimulateProperties properties;

    public PaperTradingController(PaperTradingService paperTradingService, SimulateProperties properties) {
        this.paperTradingService = paperTradingService;
        this.properties = properties;
    }

    @GetMapping("/state")
    public PaperAccountState state() {
        return paperTradingService.snapshot();
    }

    @GetMapping("/jobs/{jobId}/timeline")
    public JobTimeline timeline(@PathVariable("jobId") @Pattern(regexp = "^[A-Za-z0-9._:-]+$") String jobId) {
        JobTimeline timeline = paperTradingService.timeline(jobId);
        if (timeline.candles().isEmpty() && timeline.events().isEmpty() && timeline.balance().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown jobId: " + jobId);
        }
        return timeline;
    }

    @PostMapping("/mark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(@RequestBody MarkPriceRequest request) {
        paperTradingService.onMarkPrice(request.price());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@RequestHeader("X-Admin-Token") String token) {
        if (!properties.getAuth().getAdminToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token");
        }
        paperTradingService.reset();
    }

    public record MarkPriceRequest(@NotNull Double price) {
    }
}
