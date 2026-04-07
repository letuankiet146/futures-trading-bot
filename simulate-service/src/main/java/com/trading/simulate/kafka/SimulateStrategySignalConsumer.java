package com.trading.simulate.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.config.KafkaTopicsProperties;
import com.trading.simulate.service.LatencyPolicyService;
import com.trading.simulate.service.PaperTradingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trading.mode", havingValue = "SIMULATE", matchIfMissing = true)
public class SimulateStrategySignalConsumer {
    private static final Logger log = LoggerFactory.getLogger(SimulateStrategySignalConsumer.class);

    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topicsProperties;
    private final PaperTradingService paperTradingService;
    private final LatencyPolicyService latencyPolicyService;

    public SimulateStrategySignalConsumer(
            ObjectMapper objectMapper,
            KafkaTopicsProperties topicsProperties,
            PaperTradingService paperTradingService,
            LatencyPolicyService latencyPolicyService) {
        this.objectMapper = objectMapper;
        this.topicsProperties = topicsProperties;
        this.paperTradingService = paperTradingService;
        this.latencyPolicyService = latencyPolicyService;
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
            if (latencyPolicyService.shouldBlock(event.getCorrelationId(), event.getTimestamp())) {
                return;
            }
            log.info("SIMULATE consume signal correlationId={} symbol={} side={} topic={}",
                    correlationId, event.getSymbol(), event.getSide(), topicsProperties.getStrategySignal());
            paperTradingService.onStrategySignal(event);
        } catch (Exception ex) {
            // v1 policy from spec: log-and-skip malformed events.
            log.warn("SIMULATE log-and-skip invalid signal payload. topic={} partition={} offset={} correlationId={} payload={}",
                    record.topic(), record.partition(), record.offset(), correlationId, record.value(), ex);
        }
    }
}
