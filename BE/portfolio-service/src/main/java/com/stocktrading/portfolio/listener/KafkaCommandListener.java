package com.stocktrading.portfolio.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.stocktrading.portfolio.service.KafkaCommandHandlerService;
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
            topics = "${kafka.topics.portfolio-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePortfolioCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "PORTFOLIO_UPDATE_POSITIONS":
                    commandHandlerService.handleUpdatePositions(command);
                    break;
                case "PORTFOLIO_REMOVE_POSITIONS":
                    commandHandlerService.handleRemovePositions(command);
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