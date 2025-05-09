package com.stocktrading.orderservice.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.stocktrading.orderservice.service.KafkaCommandHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandListener {

    private final KafkaCommandHandlerService commandHandlerService;

    @KafkaListener(
            topics = "${kafka.topics.order-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "ORDER_CREATE":
                    commandHandlerService.handleCreateOrder(command);
                    break;
                case "ORDER_UPDATE_VALIDATED":
                    commandHandlerService.handleUpdateOrderValidated(command);
                    break;
                case "ORDER_UPDATE_EXECUTED":
                    commandHandlerService.handleUpdateOrderExecuted(command);
                    break;
                case "ORDER_CANCEL":
                    commandHandlerService.handleCancelOrder(command);
                    break;
                case "ORDER_UPDATE_COMPLETED":
                    commandHandlerService.handleUpdateOrderCompleted(command);
                    break;
                // Add cases for other commands as they are implemented
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Command acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Command processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.order-commands-sell}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderSellCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing sell command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "ORDER_CREATE":
                    commandHandlerService.handleCreateSellOrder(command);
                    break;
                case "ORDER_UPDATE_VALIDATED":
                    commandHandlerService.handleUpdateSellOrderValidated(command);
                    break;
                case "ORDER_UPDATE_EXECUTED":
                    commandHandlerService.handleUpdateSellOrderExecuted(command);
                    break;
                case "ORDER_CANCEL":
                    commandHandlerService.handleCancelSellOrder(command);
                    break;
                case "ORDER_UPDATE_COMPLETED":
                    commandHandlerService.handleUpdateSellOrderCompleted(command);
                    break;
                default:
                    log.warn("Unknown sell command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Sell command acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing sell command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Sell command processing failed", e);
        }
    }
}