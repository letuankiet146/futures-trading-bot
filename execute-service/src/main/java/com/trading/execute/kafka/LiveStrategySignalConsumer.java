package com.trading.execute.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.StrategySignalEvent;
import com.trading.execute.config.KafkaTopicsProperties;
import com.trading.execute.service.ExecuteAuditService;
import com.trading.execute.service.LatencyPolicyService;
import com.trading.execute.service.LiveExecutionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trading.mode", havingValue = "LIVE")
public class LiveStrategySignalConsumer {
    private static final Logger log = LoggerFactory.getLogger(LiveStrategySignalConsumer.class);

    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topicsProperties;
    private final LiveExecutionService liveExecutionService;
    private final LatencyPolicyService latencyPolicyService;
    private final ExecuteAuditService auditService;

    public LiveStrategySignalConsumer(
            ObjectMapper objectMapper,
            KafkaTopicsProperties topicsProperties,
            LiveExecutionService liveExecutionService,
            LatencyPolicyService latencyPolicyService,
            ExecuteAuditService auditService) {
        this.objectMapper = objectMapper;
        this.topicsProperties = topicsProperties;
        this.liveExecutionService = liveExecutionService;
        this.latencyPolicyService = latencyPolicyService;
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.strategy-signal}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(
            ConsumerRecord<String, String> record,
            @Header(name = "correlationId", required = false) String correlationHeader) {
        String correlationId = correlationHeader == null || correlationHeader.isBlank() ? "n/a" : correlationHeader;
        try {
            StrategySignalEvent event = objectMapper.readValue(record.value(), StrategySignalEvent.class);
            LatencyPolicyService.LatencyDecision latencyDecision =
                    latencyPolicyService.evaluate(event.getCorrelationId(), event.getTimestamp());
            if (latencyDecision.blocked()) {
                auditService.log("LATENCY_BLOCK", event.getCorrelationId(),
                        "Blocked by latency policy ms=" + latencyDecision.latencyMs());
                return;
            }
            log.info("LIVE consume signal correlationId={} symbol={} side={} topic={}",
                    correlationId, event.getSymbol(), event.getSide(), topicsProperties.getStrategySignal());
            liveExecutionService.process(event);
        } catch (Exception ex) {
            // v1 policy from spec: log-and-skip malformed events.
            log.warn("LIVE log-and-skip invalid signal payload. topic={} partition={} offset={} correlationId={} payload={}",
                    record.topic(), record.partition(), record.offset(), correlationId, record.value(), ex);
        }
    }
}
