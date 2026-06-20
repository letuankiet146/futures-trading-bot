package com.trading.simulate.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.contracts.event.SimulateReplayFeedEvent;
import com.trading.contracts.event.StrategySignalEvent;
import com.trading.simulate.config.KafkaTopicsProperties;
import com.trading.simulate.service.PaperTradingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Ordered backtest feed: MARK updates paper mark (TP/SL/liquidation); SIGNAL opens/advances like live {@code strategy.signal}.
 */
@Component
@ConditionalOnProperty(name = "trading.mode", havingValue = "SIMULATE", matchIfMissing = true)
public class SimulateReplayFeedConsumer {
    private static final Logger log = LoggerFactory.getLogger(SimulateReplayFeedConsumer.class);

    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topicsProperties;
    private final PaperTradingService paperTradingService;

    public SimulateReplayFeedConsumer(
            ObjectMapper objectMapper,
            KafkaTopicsProperties topicsProperties,
            PaperTradingService paperTradingService) {
        this.objectMapper = objectMapper;
        this.topicsProperties = topicsProperties;
        this.paperTradingService = paperTradingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.simulate-replay}",
            groupId = "${KAFKA_GROUP_SIMULATE_REPLAY:simulate-service-replay}",
            concurrency = "1")
    public void consume(
            ConsumerRecord<String, String> record,
            @Header(name = "correlationId", required = false) String correlationHeader) {
        String correlationId = correlationHeader == null || correlationHeader.isBlank() ? "n/a" : correlationHeader;
        try {
            SimulateReplayFeedEvent feed = objectMapper.readValue(record.value(), SimulateReplayFeedEvent.class);
            String type = feed.getFeedType() == null ? "" : feed.getFeedType().trim().toUpperCase();
            if (SimulateReplayFeedEvent.TYPE_MARK.equals(type)) {
                log.debug("SIMULATE replay MARK symbol={} price={}", feed.getSymbol(), feed.getPrice());
                paperTradingService.onReplayMarkPrice(feed.getPrice(), feed.getCorrelationId(), feed.getTimestamp());
                return;
            }
            if (SimulateReplayFeedEvent.TYPE_SIGNAL.equals(type)) {
                StrategySignalEvent event = new StrategySignalEvent();
                event.setSchemaVersion(feed.getSchemaVersion() != null ? feed.getSchemaVersion() : 1);
                event.setSymbol(feed.getSymbol());
                event.setSide(feed.getSide());
                event.setPrice(feed.getPrice());
                event.setTakeProfitPrice(feed.getTakeProfitPrice());
                event.setStopLossPrice(feed.getStopLossPrice());
                event.setCorrelationId(feed.getCorrelationId());
                event.setTimestamp(feed.getTimestamp());
                log.info("SIMULATE replay SIGNAL correlationId={} symbol={} side={} topic={}",
                        correlationId, event.getSymbol(), event.getSide(), topicsProperties.getSimulateReplay());
                paperTradingService.onStrategySignal(event);
                return;
            }
            log.warn("SIMULATE replay unknown feedType={} payload={}", feed.getFeedType(), record.value());
        } catch (Exception ex) {
            log.warn("SIMULATE log-and-skip invalid replay payload. topic={} partition={} offset={} correlationId={} payload={}",
                    record.topic(), record.partition(), record.offset(), correlationId, record.value(), ex);
        }
    }
}
