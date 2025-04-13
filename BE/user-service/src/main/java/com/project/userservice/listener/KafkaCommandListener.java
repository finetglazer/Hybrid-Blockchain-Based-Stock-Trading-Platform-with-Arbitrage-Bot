//package com.project.userservice.listener;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.project.userservice.model.kafka.CommandMessage;
//import com.project.userservice.service.kafka.KafkaCommandHandlerService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class KafkaCommandListener {
//
//    private final KafkaCommandHandlerService commandHandlerService;
//    private final ObjectMapper objectMapper;
//
//    @KafkaListener(
//            topics = "${kafka.topics.user-commands:user.commands.verify}",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consumeUserCommands(@Payload String commandJson, Acknowledgment ack) {
//        try {
//            log.debug("Received command JSON: {}", commandJson);
//
//            // Convert the JSON string to CommandMessage using ObjectMapper
//            // The objectMapper is already configured to handle unknown properties
//            CommandMessage command = objectMapper.readValue(commandJson, CommandMessage.class);
//
//            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());
//
//            // Route to appropriate handler based on command type
//            if ("USER_VERIFY_IDENTITY".equals(command.getType())) {
//                commandHandlerService.handleVerifyIdentityCommand(command);
//                log.info("Successfully processed USER_VERIFY_IDENTITY command");
//            } else {
//                log.warn("Unknown command type: {}", command.getType());
//            }
//
//            // Acknowledge the message
//            ack.acknowledge();
//
//        } catch (Exception e) {
//            log.error("Error processing command: {}", e.getMessage(), e);
//            // We'll acknowledge anyway to prevent infinite retries
//            // In a production environment, you might want a more sophisticated error handling strategy
//            ack.acknowledge();
//        }
//    }
//}
