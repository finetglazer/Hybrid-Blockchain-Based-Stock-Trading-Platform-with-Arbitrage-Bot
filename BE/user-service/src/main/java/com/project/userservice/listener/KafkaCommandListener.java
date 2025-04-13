
package com.project.userservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.userservice.service.kafka.KafkaCommandHandlerService;

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
    private final ObjectMapper objectMapper;

    @KafkaListener(
            id = "userCommandsListener",
            topics = "${kafka.topics.user-commands:user.commands.verify}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Handle command based on type
            if ("USER_VERIFY_IDENTITY".equals(command.getType())) {
                commandHandlerService.handleVerifyIdentityCommand(command);
                log.info("Successfully processed USER_VERIFY_IDENTITY command");
            } else {
                log.warn("Unknown command type: {}", command.getType());
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing command: {}", e.getMessage(), e);
            // For business logic errors, don't acknowledge
            throw new RuntimeException("Command processing failed", e);
        }
    }
}

