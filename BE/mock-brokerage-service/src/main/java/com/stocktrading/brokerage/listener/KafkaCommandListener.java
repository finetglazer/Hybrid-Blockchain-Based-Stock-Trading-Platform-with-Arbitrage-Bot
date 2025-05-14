package com.stocktrading.brokerage.listener;

import com.project.kafkamessagemodels.model.CommandMessage;

import com.stocktrading.brokerage.service.KafkaCommandHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener for broker commands from Kafka
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandListener {

    private final KafkaCommandHandlerService commandHandlerService;

    @KafkaListener(
            topics = "${kafka.topics.broker-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBrokerCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received broker command: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "BROKER_EXECUTE_ORDER":
                    commandHandlerService.handleExecuteOrder(command, false); // false indicates buy order
                    break;
                case "BROKER_CANCEL_ORDER":
                    commandHandlerService.handleCancelOrder(command, false); // false indicates buy order
                    break;
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Command processed and acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing broker command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ by the error handler
            throw new RuntimeException("Broker command processing failed", e);
        }
    }

    /**
     * Listener for sell order broker commands
     */
    @KafkaListener(
            topics = "${kafka.topics.broker-commands.sell}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBrokerSellCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received broker sell command: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "BROKER_EXECUTE_ORDER":
                    commandHandlerService.handleExecuteOrder(command, true); // true indicates sell order
                    break;
                case "BROKER_CANCEL_ORDER":
                    commandHandlerService.handleCancelOrder(command, true); // true indicates sell order
                    break;
                default:
                    log.warn("Unknown sell command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Sell command processed and acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing broker sell command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ by the error handler
            throw new RuntimeException("Broker sell command processing failed", e);
        }
    }
}