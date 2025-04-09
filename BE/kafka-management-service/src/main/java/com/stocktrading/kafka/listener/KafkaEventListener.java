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
    
    /**
     * Listen for DLQ messages for monitoring
     */
    @KafkaListener(
        topics = "${kafka.topics.dlq}",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeDlqMessages(@Payload EventMessage event, Acknowledgment ack) {
        log.warn("Received message in DLQ: {} for saga: {}", event.getType(), event.getSagaId());
        
        // Just acknowledge the message, we can't do much with DLQ messages
        // They're mainly for monitoring and manual intervention
        ack.acknowledge();
    }
}
