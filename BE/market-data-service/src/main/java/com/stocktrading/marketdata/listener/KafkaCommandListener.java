package com.stocktrading.marketdata.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.stocktrading.marketdata.service.KafkaCommandHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// KafkaCommandListener.java
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandListener {

    private final KafkaCommandHandlerService commandHandlerService;

    @KafkaListener(
            topics = "${kafka.topics.market-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMarketCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing market command: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "MARKET_VALIDATE_STOCK":
                    commandHandlerService.handleValidateStock(command);
                    break;
                case "MARKET_GET_PRICE":
                    commandHandlerService.handleGetPrice(command);
                    break;
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
}