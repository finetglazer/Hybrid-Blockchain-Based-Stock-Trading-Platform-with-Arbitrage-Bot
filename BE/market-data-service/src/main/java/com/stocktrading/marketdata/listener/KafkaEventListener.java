package com.stocktrading.marketdata.listener;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.marketdata.handler.StockDataWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventListener.class);

    private final StockDataWebSocketHandler webSocketHandler;

    /**
     * Listens to market price data events and forwards them to the WebSocket handler
     * This listener operates on a different topic than the one in StockDataWebSocketHandler
     */
    @KafkaListener(
            topics = "market.price.data",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-market-data"
    )
    public void listenStockUpdates(EventMessage event, Acknowledgment ack) {
        try {
            logger.debug("Received event message from market.price.data topic: {}", event.getType());

            // Forward the event message to the WebSocketHandler for processing
            // This effectively delegates to the same processing logic
            webSocketHandler.processAndBroadcastEvent(event);

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing stock update received from Kafka: ", e);
            // Acknowledge despite error to prevent endless reprocessing
            ack.acknowledge();
        }
    }
}