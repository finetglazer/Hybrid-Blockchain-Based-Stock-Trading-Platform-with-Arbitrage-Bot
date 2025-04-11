package com.accountservice.service.kafka;

import com.accountservice.model.PaymentMethod;
import com.accountservice.repository.PaymentMethodRepository;
import com.accountservice.service.PaymentMethodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final PaymentMethodService paymentMethodService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentMethodRepository paymentMethodRepository;

    /**
     * Handle PAYMENT_METHOD_VALIDATE command
     */
    public void handleValidatePaymentMethod(CommandMessage command) {
        log.info("Handling PAYMENT_METHOD_VALIDATE command for saga: {}", command.getSagaId());

        String paymentMethodId = command.getPayloadValue("paymentMethodId");
        String userId = command.getPayloadValue("userId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ACCOUNT_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Validate the payment method
            PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElse(null);

            // Check if payment method exists and belongs to user
            if (paymentMethod == null) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_FOUND",
                        "Payment method not found: " + paymentMethodId);
                return;
            }

            if (!paymentMethod.getUserId().equals(userId)) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_AUTHORIZED",
                        "Payment method does not belong to user");
                return;
            }

            // Check status
            if (!paymentMethod.getStatus().equals(PaymentMethod.PaymentMethodStatus.ACTIVE.toString())) {
                handleValidationFailure(event, "PAYMENT_METHOD_NOT_ACTIVE",
                        "Payment method is not active: " + paymentMethod.getStatus());
                return;
            }

            // All validations passed
            event.setType("PAYMENT_METHOD_VALID");
            event.setSuccess(true);
            event.setPayloadValue("paymentMethodId", paymentMethodId);
            event.setPayloadValue("paymentMethodType", paymentMethod.getType().toString());
            event.setPayloadValue("paymentMethodName", paymentMethod.getNickname());

        } catch (Exception e) {
            log.error("Error validating payment method", e);
            handleValidationFailure(event, "VALIDATION_ERROR",
                    "Error validating payment method: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send("account.events.deposit", command.getSagaId(), event);
            log.info("Sent PAYMENT_METHOD_VALID response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle validation failures
     */
    private void handleValidationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("PAYMENT_METHOD_INVALID");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send("account.events.deposit", event.getSagaId(), event);
            log.info("Sent PAYMENT_METHOD_INVALID response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
}