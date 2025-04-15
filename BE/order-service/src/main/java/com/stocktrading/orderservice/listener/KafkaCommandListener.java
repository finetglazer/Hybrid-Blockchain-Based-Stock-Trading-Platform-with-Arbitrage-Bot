package com.stocktrading.orderservice.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for handling order commands
 * This is a placeholder - you'll implement the actual command handling later
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandListener {

    // TODO: Add KafkaCommandHandlerService later

    @KafkaListener(
            topics = "${kafka.topics.order-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received order command: {}", command.getType());

            // For now, just log the command and acknowledge it
            // You'll implement actual command handling later
            log.info("Command details: sagaId={}, type={}, isCompensation={}",
                    command.getSagaId(), command.getType(), command.getIsCompensation());

            ack.acknowledge();
            log.debug("Command acknowledged");

        } catch (Exception e) {
            log.error("Error processing order command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
        }
    }
}