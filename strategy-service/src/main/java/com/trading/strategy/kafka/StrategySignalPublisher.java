package com.trading.strategy.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.StrategySignalEvent;
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

@Service
public class StrategySignalPublisher {
    private static final Logger log = LoggerFactory.getLogger(StrategySignalPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final KafkaTopicsProperties topicsProperties;

    public StrategySignalPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Validator validator,
            KafkaTopicsProperties topicsProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.topicsProperties = topicsProperties;
    }

    public StrategySignalEvent publishSignal(
            String symbol, String side, double price, Double takeProfitPrice, Double stopLossPrice) {
        StrategySignalEvent event = new StrategySignalEvent();
        event.setSchemaVersion(2);
        event.setSymbol(symbol);
        event.setSide(side);
        event.setPrice(price);
        event.setTakeProfitPrice(takeProfitPrice);
        event.setStopLossPrice(stopLossPrice);
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now().toString());

        Set<ConstraintViolation<StrategySignalEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid strategy signal payload: " + violations);
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, topicsProperties.getStrategySignal())
                    .setHeader(KafkaHeaders.KEY, event.getSymbol())
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();
            kafkaTemplate.send(message);
            log.info("Published strategy signal correlationId={} symbol={} side={} topic={}",
                    event.getCorrelationId(), event.getSymbol(), event.getSide(), topicsProperties.getStrategySignal());
            return event;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize strategy signal event", e);
        }
    }
}
