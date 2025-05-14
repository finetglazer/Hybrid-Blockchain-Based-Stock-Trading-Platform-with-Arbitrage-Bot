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
            id = "accountCommonCommandsListener",
            topics = "${kafka.topics.account-commands.common}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountCommonCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            switch (command.getType()) {
                case "ACCOUNT_VALIDATE":
                    commandHandlerService.handleAccountValidation(command);
                    break;
                case "PAYMENT_METHOD_VALIDATE":
                    commandHandlerService.handleValidatePaymentMethod(command);
                    break;
                case "ACCOUNT_CHECK_BALANCE":
                    commandHandlerService.handleCheckBalance(command);
                    break;
                case "ACCOUNT_UPDATE_TRANSACTION_STATUS":
                    commandHandlerService.handleUpdateTransactionStatus(command);
                    break;
                // Add handlers for compensation commands
                case "ACCOUNT_MARK_TRANSACTION_FAILED":
                    commandHandlerService.handleMarkTransactionFailed(command);
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

    @KafkaListener(
            id = "accountDepositCommandsListener",
            topics = "${kafka.topics.account-commands.deposit}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountDepositCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION":
                    commandHandlerService.handleCreateDepositPendingTransaction(command);
                    break;
                case "ACCOUNT_DEPOSIT_UPDATE_BALANCE":
                    commandHandlerService.handleDepositUpdateBalance(command);
                    break;
                // Add handlers for compensation commands
                case "ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE":
                    commandHandlerService.handleDepositReverseBalanceUpdate(command);
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

    @KafkaListener(
            id = "accountWithdrawalCommandsListener",
            topics = "${kafka.topics.account-commands.withdrawal}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountWithdrawalCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing command type: {} for saga: {}", command.getType(), command.getSagaId());

            switch (command.getType()) {
                case "ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION":
                    commandHandlerService.handleCreateWithdrawalPendingTransaction(command);
                    break;
                case "ACCOUNT_WITHDRAWAL_UPDATE_BALANCE":
                    commandHandlerService.handleWithdrawalUpdateBalance(command);
                    break;
                // Add handlers for compensation commands
                case "ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE":
                    commandHandlerService.handleWithdrawalReverseBalanceUpdate(command);
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

    // In BE/account-service/src/main/java/com/accountservice/listener/KafkaCommandListener.java

    @KafkaListener(
            id = "accountOrderCommandsListener",
            topics = "${kafka.topics.account-commands.order-buy}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountOrderCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing order command type: {} for saga: {}", command.getType(), command.getSagaId());

            switch (command.getType()) {
                case "ACCOUNT_VERIFY_STATUS":
                    commandHandlerService.handleVerifyAccountStatus(command);
                    break;
                case "ACCOUNT_RESERVE_FUNDS":
                    commandHandlerService.handleReserveFunds(command);
                    break;
                case "ACCOUNT_RELEASE_FUNDS":
                    commandHandlerService.handleReleaseFunds(command);
                    break;
                case "ACCOUNT_SETTLE_TRANSACTION":  // Add this case
                    commandHandlerService.handleSettleTransaction(command);
                    break;
                case "ACCOUNT_REVERSE_SETTLEMENT":  // Add this for compensation
                    commandHandlerService.handleReverseSettlement(command);
                    break;
                default:
                    log.warn("Unknown account order command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing account order command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Command processing failed", e);
        }
    }

    @KafkaListener(
            id = "accountOrderSellCommandsListener",
            topics = "${kafka.topics.account-commands.order-sell}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountOrderSellCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Processing account sell command type: {} for saga: {}", command.getType(), command.getSagaId());

            switch (command.getType()) {
                case "ACCOUNT_VERIFY_STATUS":
                    commandHandlerService.handleVerifyAccountStatusSell(command);
                    break;
                case "ACCOUNT_SETTLE_TRANSACTION":  // Add this case
                    commandHandlerService.handleSettleTransactionSell(command);
                    break;
                case "ACCOUNT_REVERSE_SETTLEMENT":  // Add this for compensation
                    commandHandlerService.handleReverseSettlementSell(command);
                    break;
                default:
                    log.warn("Unknown account sell command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing account sell command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Command processing failed", e);
        }
    }
}