package com.accountservice.listener;

import com.accountservice.service.kafka.KafkaCommandHandlerService;
import com.project.kafkamessagemodels.model.CommandMessage;
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
            id = "accountCommandsListener",
            topics = "${kafka.topics.account-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "ACCOUNT_VALIDATE":
                    commandHandlerService.handleAccountValidation(command);
                    break;
                case "PAYMENT_METHOD_VALIDATE":
                    commandHandlerService.handleValidatePaymentMethod(command);
                    break;
                case "ACCOUNT_CREATE_PENDING_TRANSACTION":
                    commandHandlerService.handleCreatePendingTransaction(command);
                    break;
                case "ACCOUNT_UPDATE_TRANSACTION_STATUS":
                    commandHandlerService.handleUpdateTransactionStatus(command);
                    break;
                case "ACCOUNT_UPDATE_BALANCE":
                    commandHandlerService.handleUpdateBalance(command);
                    break;
                // Add handlers for compensation commands
                case "ACCOUNT_MARK_TRANSACTION_FAILED":
                    commandHandlerService.handleMarkTransactionFailed(command);
                    break;
                case "ACCOUNT_REVERSE_BALANCE_UPDATE":
                    commandHandlerService.handleReverseBalanceUpdate(command);
                    break;
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Command processing failed", e);
        }
    }
}