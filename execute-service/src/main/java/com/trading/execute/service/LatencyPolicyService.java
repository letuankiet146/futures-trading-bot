package com.trading.execute.service;

import com.trading.execute.config.ExecuteProperties;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LatencyPolicyService {
    private static final Logger log = LoggerFactory.getLogger(LatencyPolicyService.class);
    private final ExecuteProperties executeProperties;

    public LatencyPolicyService(ExecuteProperties executeProperties) {
        this.executeProperties = executeProperties;
    }

    public LatencyDecision evaluate(String correlationId, String eventTimestamp) {
        try {
            long latencyMs = Duration.between(Instant.parse(eventTimestamp), Instant.now()).toMillis();
            if (latencyMs >= executeProperties.getLatency().getBlockMs()) {
                log.warn("Latency hard-block correlationId={} latencyMs={} thresholdMs={}",
                        correlationId, latencyMs, executeProperties.getLatency().getBlockMs());
                return new LatencyDecision(latencyMs, true);
            }
            if (latencyMs > executeProperties.getLatency().getWarnMs()) {
                log.warn("Latency warning correlationId={} latencyMs={} thresholdMs={}",
                        correlationId, latencyMs, executeProperties.getLatency().getWarnMs());
            }
            return new LatencyDecision(latencyMs, false);
        } catch (Exception e) {
            log.warn("Failed to parse event timestamp. allow by default correlationId={} timestamp={}", correlationId, eventTimestamp);
            return new LatencyDecision(-1L, false);
        }
    }

    public record LatencyDecision(long latencyMs, boolean blocked) {
    }
}
