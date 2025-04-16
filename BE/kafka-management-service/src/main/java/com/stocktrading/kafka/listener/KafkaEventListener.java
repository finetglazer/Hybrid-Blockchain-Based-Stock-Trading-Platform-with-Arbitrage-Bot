package com.stocktrading.kafka.listener;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.kafka.service.DepositSagaService;
import com.stocktrading.kafka.service.IdempotencyService;
import com.stocktrading.kafka.service.OrderBuySagaService;
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
    private final OrderBuySagaService orderBuySagaService;
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
// In KafkaEventListener.java in Kafka Management Service
// In KafkaEventListener.java

    @KafkaListener(
            topics = "${kafka.topics.user-events}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeUserEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received user event: {}", event.getType());

            // --- ADD THIS LINE ---
            // Call the service to handle the event and advance the saga
            depositSagaService.handleEventMessage(event);
            // --------------------

            // Acknowledge only after successful processing by the service
            ack.acknowledge();
            log.debug("Successfully processed and acknowledged user event: {}", event.getType());

        } catch (Exception e) {
            log.error("Error processing user event: {} for saga: {}", event.getType(), event.getSagaId(), e);
            // Let the container's error handler deal with this exception (retries/DLQ).
            // Do NOT acknowledge here.
            // Re-throwing is one way to ensure the container's error handler takes over.
            throw new RuntimeException("Error processing user event " + event.getType() + " for saga " + event.getSagaId(), e);
        }
    }

    // Find this method in the KafkaEventListener.java file of the Kafka Management Service
// and replace it with the version below:

    /**
     * Listen for DLQ messages for monitoring
     */
    /**
     * Listen for DLQ messages for monitoring
     */
    @KafkaListener(
            topics = "${kafka.topics.dlq}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    // Change the payload type from String to Object
    public void consumeDlqMessages(@Payload Object messagePayload, Acknowledgment ack) { // <-- Change String to Object
        try {
            // Log the received object (its toString() representation)
            log.warn("Received raw message object in DLQ: {}", messagePayload);

            // Optional: If you need the JSON string representation, you can use ObjectMapper
            // try {
            //     ObjectMapper objectMapper = new ObjectMapper();
            //     // Configure object mapper if necessary (e.g., JavaTimeModule)
            //     objectMapper.registerModule(new JavaTimeModule());
            //     objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            //     String messageJson = objectMapper.writeValueAsString(messagePayload);
            //     log.warn("Received message in DLQ (converted to JSON): {}", messageJson);
            // } catch (Exception jsonEx) {
            //     log.error("Could not convert DLQ message payload to JSON string", jsonEx);
            // }

            // Acknowledge the message - we're mostly logging for monitoring
            ack.acknowledge();
            log.info("Acknowledged DLQ message");
        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop in DLQ processing
            ack.acknowledge();
        }
    }

    /**
     * Listen for order service events
     */
    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeOrderEvents(@Payload EventMessage event, Acknowledgment ack) {
        try {
            log.debug("Received order event: {}", event.getType());

            // Handle the event - this line calls the OrderBuySagaService
            orderBuySagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Successfully processed and acknowledged order event: {}", event.getType());

        } catch (Exception e) {
            log.error("Error processing order event: {} for saga: {}", event.getType(), event.getSagaId(), e);
            // Don't ack, will be retried or sent to DLQ by error handler
            throw new RuntimeException("Error processing order event", e);
        }
    }


}