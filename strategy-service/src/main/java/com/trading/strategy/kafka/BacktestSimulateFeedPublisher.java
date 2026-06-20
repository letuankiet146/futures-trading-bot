package com.trading.strategy.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.SimulateReplayFeedEvent;
import com.trading.strategy.config.KafkaTopicsProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
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

    public void publishMark(String symbol, double price, String correlationId, String timestamp) {
        SimulateReplayFeedEvent event = baseEvent(symbol, price, SimulateReplayFeedEvent.TYPE_MARK, timestamp);
        event.setCorrelationId(correlationId);
        send(event, correlationId);
    }

    public void publishSignal(
            String symbol,
            String side,
            double price,
            double takeProfitPrice,
            double stopLossPrice,
            String correlationId,
            String timestamp) {
        SimulateReplayFeedEvent event = baseEvent(symbol, price, SimulateReplayFeedEvent.TYPE_SIGNAL, timestamp);
        event.setSide(side);
        event.setCorrelationId(correlationId);
        event.setTakeProfitPrice(takeProfitPrice);
        event.setStopLossPrice(stopLossPrice);
        send(event, correlationId);
    }

    private static SimulateReplayFeedEvent baseEvent(String symbol, double price, String feedType, String timestamp) {
        SimulateReplayFeedEvent event = new SimulateReplayFeedEvent();
        event.setSchemaVersion(2);
        event.setFeedType(feedType);
        event.setSymbol(symbol);
        event.setPrice(price);
        event.setTimestamp(timestamp);
        return event;
    }

    private void send(SimulateReplayFeedEvent event, String correlationHeader) {
        Set<ConstraintViolation<SimulateReplayFeedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid simulate replay payload: " + violations);
        }
        boolean needsCorrelationId = SimulateReplayFeedEvent.TYPE_SIGNAL.equals(event.getFeedType())
                || SimulateReplayFeedEvent.TYPE_MARK.equals(event.getFeedType());
        if (needsCorrelationId && (event.getCorrelationId() == null || event.getCorrelationId().isBlank())) {
            throw new IllegalArgumentException("Replay rows require correlationId");
        }
        if (SimulateReplayFeedEvent.TYPE_SIGNAL.equals(event.getFeedType())
                && (event.getSide() == null || event.getSide().isBlank())) {
            throw new IllegalArgumentException("SIGNAL replay rows require side");
        }
        if (SimulateReplayFeedEvent.TYPE_SIGNAL.equals(event.getFeedType())
                && (event.getTakeProfitPrice() == null || event.getStopLossPrice() == null)) {
            throw new IllegalArgumentException("SIGNAL replay rows require takeProfitPrice and stopLossPrice");
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
