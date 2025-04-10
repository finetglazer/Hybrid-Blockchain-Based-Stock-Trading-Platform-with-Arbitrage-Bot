package com.stocktrading.kafka.listener;

import com.stocktrading.kafka.model.EventMessage;
import com.stocktrading.kafka.service.DepositSagaService;
import com.stocktrading.kafka.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for processing event messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final DepositSagaService depositSagaService;
    private final IdempotencyService idempotencyService;

    /**
     * Listen for account service events
     */
    @KafkaListener(
            topics = "${kafka.topics.account-events}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeAccountEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received account event: {}", event.getType());

            // Handle the event
            depositSagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing account event: {}", event.getType(), e);
            // Don't ack, will be retried or sent to DLQ by error handler
        }
    }

    /**
     * Listen for payment service events
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received payment event: {}", event.getType());

            // Handle the event
            depositSagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing payment event: {}", event.getType(), e);
            // Don't ack, will be retried or sent to DLQ by error handler
        }
    }

    /**
     * Listen for user service events
     */
    @KafkaListener(
            topics = "${kafka.topics.user-events}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeUserEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received user event: {}", event.getType());

            // Handle the event
            depositSagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing user event: {}", event.getType(), e);
            // Don't ack, will be retried or sent to DLQ by error handler
        }
    }

    // Find this method in the KafkaEventListener.java file of the Kafka Management Service
// and replace it with the version below:

    /**
     * Listen for DLQ messages for monitoring
     */
    @KafkaListener(
            topics = "${kafka.topics.dlq}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeDlqMessages(@Payload String messageJson, Acknowledgment ack) {
        try {
            log.warn("Received message in DLQ: {}", messageJson);

            // We could parse the message here if needed
            // ObjectMapper objectMapper = new ObjectMapper();
            // Map<String, Object> messageMap = objectMapper.readValue(messageJson,
            //    new TypeReference<Map<String, Object>>() {});

            // Just acknowledge the message - we're mostly logging for monitoring
            ack.acknowledge();
            log.info("Acknowledged DLQ message");
        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop
            ack.acknowledge();
        }
    }
}