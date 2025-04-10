package com.project.userservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.model.kafka.CommandMessage;
import com.project.userservice.service.kafka.KafkaCommandHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandListener {

    private final KafkaCommandHandlerService commandHandlerService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.user-commands:user.commands.verify}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserCommands(@Payload String commandJson, Acknowledgment ack) {
        try {
            log.debug("Received command JSON: {}", commandJson);

            // Parse JSON to Map
            Map<String, Object> commandMap = objectMapper.readValue(commandJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            String commandType = (String) commandMap.get("type");
            String sagaId = (String) commandMap.get("sagaId");
            log.info("Processing command type: {} for saga: {}", commandType, sagaId);

            // Convert the map to a CommandMessage
            CommandMessage command = objectMapper.convertValue(commandMap, CommandMessage.class);

            // Route to appropriate handler based on command type
            if ("USER_VERIFY_IDENTITY".equals(commandType)) {
                commandHandlerService.handleVerifyIdentityCommand(command);
                log.info("Successfully processed USER_VERIFY_IDENTITY command");
            } else {
                log.warn("Unknown command type: {}", commandType);
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing command: {}", e.getMessage(), e);
            // We should properly handle errors - either retry or send to DLQ
            // For now, acknowledging to prevent infinite retries
            ack.acknowledge();
        }
    }
}