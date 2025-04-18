// src/main/java/com/stocktrading/payment/listener/KafkaCommandListener.java
package com.stocktrading.payment.listener;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.stocktrading.payment.service.PaymentProcessorService;
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

    private final PaymentProcessorService paymentProcessorService;

    @KafkaListener(
            topics = "${kafka.topics.payment-commands}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentCommands(@Payload CommandMessage command, Acknowledgment ack) {
        try {
            log.info("Received payment command: {}", command.getType());

            // Route to appropriate handler based on command type
            switch (command.getType()) {
                case "PAYMENT_PROCESS_DEPOSIT":
                    paymentProcessorService.processDeposit(command);
                    break;
                case "PAYMENT_REVERSE_DEPOSIT":
                    paymentProcessorService.reverseDeposit(command);
                case "PAYMENT_PROCESS_WITHDRAWAL":
                    paymentProcessorService.processWithdrawal(command);
                case "PAYMENT_REVERSE_WITHDRAWAL":
                    paymentProcessorService.reverseWithdrawal(command);
                    break;
                default:
                    log.warn("Unknown command type: {}", command.getType());
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Command processed and acknowledged: {}", command.getType());

        } catch (Exception e) {
            log.error("Error processing payment command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ by the error handler
            throw new RuntimeException("Payment command processing failed", e);
        }
    }
}
