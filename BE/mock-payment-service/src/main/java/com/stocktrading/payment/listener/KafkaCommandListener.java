// src/main/java/com/stocktrading/payment/listener/KafkaCommandListener.java
package com.stocktrading.payment.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.stocktrading.payment.service.DepositPaymentProcessorService;
import com.stocktrading.payment.service.WithdrawalPaymentProcessorService;
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

    private final DepositPaymentProcessorService depositPaymentProcessorService;
    private final WithdrawalPaymentProcessorService withdrawalPaymentProcessorService;

    @KafkaListener(
            topics = "${kafka.topics.payment-commands.deposit}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDepositPaymentCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received deposit payment command: {}", command.getType());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "PAYMENT_PROCESS_DEPOSIT":
                    depositPaymentProcessorService.processDeposit(command);
                    break;
                case "PAYMENT_REVERSE_DEPOSIT":
                    depositPaymentProcessorService.reverseDeposit(command);
                    break;
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Command processed and acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing deposit payment command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ by the error handler
            throw new RuntimeException("Deposit payment command processing failed", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-commands.withdrawal}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWithdrawalPaymentCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received withdrawal payment command: {}", command.getType());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "PAYMENT_PROCESS_WITHDRAWAL":
                    withdrawalPaymentProcessorService.processWithdrawal(command);
                    break;
                case "PAYMENT_REVERSE_WITHDRAWAL":
                    withdrawalPaymentProcessorService.reverseWithdrawal(command);
                    break;
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Command processed and acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing withdrawal payment command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ by the error handler
            throw new RuntimeException("Withdrawal payment command processing failed", e);
        }
    }
}
