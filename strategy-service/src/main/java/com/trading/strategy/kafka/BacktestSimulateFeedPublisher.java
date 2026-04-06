package com.trading.strategy.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.SimulateReplayFeedEvent;
import com.trading.strategy.config.KafkaTopicsProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Publishes ordered MARK (per OHLC step) and SIGNAL rows to the simulate replay topic during backtest.
 */
@Service
public class BacktestSimulateFeedPublisher {
    private static final Logger log = LoggerFactory.getLogger(BacktestSimulateFeedPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final KafkaTopicsProperties topicsProperties;

    public BacktestSimulateFeedPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Validator validator,
            KafkaTopicsProperties topicsProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.topicsProperties = topicsProperties;
    }

    public void publishMark(String symbol, double price) {
        SimulateReplayFeedEvent event = baseEvent(symbol, price, SimulateReplayFeedEvent.TYPE_MARK);
        send(event, null);
    }

    public void publishSignal(String symbol, String side, double price) {
        SimulateReplayFeedEvent event = baseEvent(symbol, price, SimulateReplayFeedEvent.TYPE_SIGNAL);
        event.setSide(side);
        event.setCorrelationId(UUID.randomUUID().toString());
        send(event, event.getCorrelationId());
    }

    private static SimulateReplayFeedEvent baseEvent(String symbol, double price, String feedType) {
        SimulateReplayFeedEvent event = new SimulateReplayFeedEvent();
        event.setSchemaVersion(1);
        event.setFeedType(feedType);
        event.setSymbol(symbol);
        event.setPrice(price);
        event.setTimestamp(Instant.now().toString());
        return event;
    }

    private void send(SimulateReplayFeedEvent event, String correlationHeader) {
        Set<ConstraintViolation<SimulateReplayFeedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid simulate replay payload: " + violations);
        }
        if (SimulateReplayFeedEvent.TYPE_SIGNAL.equals(event.getFeedType())) {
            if (event.getSide() == null || event.getSide().isBlank() || event.getCorrelationId() == null) {
                throw new IllegalArgumentException("SIGNAL replay rows require side and correlationId");
            }
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            MessageBuilder<String> builder = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, topicsProperties.getSimulateReplay())
                    .setHeader(KafkaHeaders.KEY, event.getSymbol());
            if (correlationHeader != null) {
                builder.setHeader("correlationId", correlationHeader);
            }
            kafkaTemplate.send(builder.build());
            log.debug("Published simulate replay feedType={} symbol={} price={} topic={}",
                    event.getFeedType(), event.getSymbol(), event.getPrice(), topicsProperties.getSimulateReplay());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize simulate replay event", e);
        }
    }

    /** Ensures MARK/SIGNAL rows reach the broker in order before the next OHLC step. */
    public void flush() {
        kafkaTemplate.flush();
    }
}
