// src/main/java/com/stocktrading/payment/service/PaymentProcessorService.java
package com.stocktrading.payment.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class WithdrawalPaymentProcessorService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    @Value("${kafka.topics.payment-events.withdrawal}")
    private String withdrawalEventsTopic;

    /**
     * Process a withdrawal payment command
     */
    public void processWithdrawal(CommandMessage command) {
        log.info("Processing withdrawal payment for saga: {}", command.getSagaId());

        String paymentMethodId = command.getPayloadValue("paymentMethodId");
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);
        String currency = command.getPayloadValue("currency");
        String accountId = command.getPayloadValue("accountId");
        String transactionId = command.getPayloadValue("transactionId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PAYMENT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Simulate payment processing delay (randomly between 500-1500ms)
            simulateProcessingDelay();

            // Simulate random failure (10% chance)
            if (shouldSimulateFailure()) {
                handleWithdrawalPaymentFailure(event, "PAYMENT_PROCESSING_ERROR", "Simulated payment processing failure");
                return;
            }

            // Generate a mock payment reference
            String paymentReference = generatePaymentReference();

            // Set success response
            event.setType("WITHDRAWAL_PAYMENT_PROCESSED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", transactionId);
            event.setPayloadValue("accountId", accountId);
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("currency", currency);
            event.setPayloadValue("paymentMethodId", paymentMethodId);
            event.setPayloadValue("processedAt", Instant.now().toString());
            event.setPayloadValue("paymentReference", paymentReference);

            log.info("Payment processed successfully with reference: {}", paymentReference);

        } catch (Exception e) {
            log.error("Error processing payment", e);
            handleWithdrawalPaymentFailure(event, "PAYMENT_PROCESSING_ERROR",
                    "Error processing payment: " + e.getMessage());
            return;
        }

        // Send the response event
        publishEvent(event);
    }

    /**
     * Process a withdrawal reversal command (compensation)
     */
    public void reverseWithdrawal(CommandMessage command) {
        log.info("Processing withdrawal reversal for saga: {}", command.getSagaId());

        String paymentReference = command.getPayloadValue("paymentReference");
        Object amountObj = command.getPayloadValue("amount");
        BigDecimal amount = convertToBigDecimal(amountObj);
        String reason = command.getPayloadValue("reason");
        String transactionId = command.getPayloadValue("transactionId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("PAYMENT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Simulate processing delay
            simulateProcessingDelay();

            // Simulate rare reversal failure (5% chance)
            if (random.nextInt(100) < 5) {
                handleWithdrawalReversalPaymentFailure(event, "REVERSAL_ERROR", "Simulated reversal failure");
                return;
            }

            // Set success response
            event.setType("WITHDRAWAL_PAYMENT_REVERSAL_COMPLETED");
            event.setSuccess(true);
            event.setPayloadValue("transactionId", transactionId);
            event.setPayloadValue("originalPaymentReference", paymentReference);
            event.setPayloadValue("reversalReference", generatePaymentReference());
            event.setPayloadValue("amount", amount);
            event.setPayloadValue("reason", reason);
            event.setPayloadValue("reversedAt", Instant.now().toString());

            log.info("Payment reversal completed for reference: {}", paymentReference);

        } catch (Exception e) {
            log.error("Error processing payment reversal", e);
            handleWithdrawalReversalPaymentFailure(event, "REVERSAL_ERROR",
                    "Error processing payment reversal: " + e.getMessage());
            return;
        }

        // Send the response event
        publishEvent(event);
    }

    /**
     * Helper method to convert any numeric type to BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object amountObj) {
        if (amountObj instanceof BigDecimal) {
            return (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            return BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
    }

    /**
     * Helper method to handle withdrawal payment failure
     */
    private void handleWithdrawalPaymentFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("WITHDRAWAL_PAYMENT_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        log.warn("Payment failed: {} - {}", errorCode, errorMessage);
        publishEvent(event);
    }

    /**
     * Helper method to handle withdrawal reversal payment failure
     */
    public void handleWithdrawalReversalPaymentFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("WITHDRAWAL_PAYMENT_REVERSAL_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        log.warn("Payment failed: {} - {}", errorCode, errorMessage);
        publishEvent(event);
    }

    /**
     * Publish event to Kafka
     */
    private void publishEvent(EventMessage event) {
        try {
            kafkaTemplate.send(withdrawalEventsTopic, event.getSagaId(), event);
            log.debug("Published event: {} for saga: {}", event.getType(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error publishing event to Kafka", e);
        }
    }

    /**
     * Generate a mock payment reference
     */
    private String generatePaymentReference() {
        return "PAYREF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Simulate payment processing delay
     */
    private void simulateProcessingDelay() {
        try {
            // Random delay between 500-1500ms
            TimeUnit.MILLISECONDS.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Should we simulate a failure? (10% chance)
     */
    private boolean shouldSimulateFailure() {
        return random.nextInt(100) < 10;
    }
}
