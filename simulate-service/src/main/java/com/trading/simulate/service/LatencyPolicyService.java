package com.trading.simulate.service;

import com.trading.simulate.config.SimulateProperties;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LatencyPolicyService {
    private static final Logger log = LoggerFactory.getLogger(LatencyPolicyService.class);
    private final SimulateProperties properties;

    public LatencyPolicyService(SimulateProperties properties) {
        this.properties = properties;
    }

    public boolean shouldBlock(String correlationId, String eventTimestamp) {
        try {
            long latencyMs = Duration.between(Instant.parse(eventTimestamp), Instant.now()).toMillis();
            if (latencyMs >= properties.getLatency().getBlockMs()) {
                log.warn("SIM latency hard-block correlationId={} latencyMs={} thresholdMs={}",
                        correlationId, latencyMs, properties.getLatency().getBlockMs());
                return true;
            }
            if (latencyMs > properties.getLatency().getWarnMs()) {
                log.warn("SIM latency warning correlationId={} latencyMs={} thresholdMs={}",
                        correlationId, latencyMs, properties.getLatency().getWarnMs());
            }
            return false;
        } catch (Exception e) {
            log.warn("SIM latency parse failure. allow by default correlationId={} timestamp={}", correlationId, eventTimestamp);
            return false;
        }
    }
}
