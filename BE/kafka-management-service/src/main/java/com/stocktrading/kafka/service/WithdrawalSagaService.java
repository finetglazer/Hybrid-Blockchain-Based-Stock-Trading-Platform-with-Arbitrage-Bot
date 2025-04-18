package com.stocktrading.kafka.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.project.kafkamessagemodels.model.enums.EventType;
import com.stocktrading.kafka.model.SagaEvent;
import com.stocktrading.kafka.model.WithdrawalSagaState;
import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.model.enums.WithdrawalSagaStep;
import com.stocktrading.kafka.repository.WithdrawalSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalSagaService {
    private final WithdrawalSagaRepository withdrawalSagaRepository;

    private final KafkaMessagePublisher kafkaMessagePublisher;

    private final IdempotencyService idempotencyService;

    @Value("${saga.deposit.retry.max-attempts}")
    private int maxRetries;

    public WithdrawalSagaState startWithdrawalSaga(String userId, String accountId, BigDecimal amount, String currency,
                                                   String paymentMethodId, String description) {
        String sagaId = UUID.randomUUID().toString();
        log.debug("Starting saga with ID: {}, amount: {} (type: {})",
            sagaId, amount, amount.getClass().getName());

        WithdrawalSagaState saga = WithdrawalSagaState.initiate(
            sagaId, userId, accountId, amount, currency, paymentMethodId, maxRetries, description
        );

        log.debug("Saga before save: id={}, amount={} (type={})",
            saga.getSagaId(), saga.getAmount(), saga.getAmount().getClass().getName()
        );
        withdrawalSagaRepository.save(saga);
        nextSagaStep(saga);

        return saga;
    }

    public void nextSagaStep(WithdrawalSagaState saga) {
        WithdrawalSagaStep nextStep = getNextStep(saga.getCurrentStep());
        CommandMessage command = new CommandMessage();
        command.setSagaId(saga.getSagaId());
        command.setStepId(nextStep.getStepNumber());
        command.setSourceService("SAGA_ORCHESTRATOR");
        command.setTimestamp(Instant.now());
        command.setType(nextStep.name());

        if (nextStep == WithdrawalSagaStep.USER_VERIFY_IDENTITY) {
            command.setTargetService("USER_SERVICE");
            command.setPayloadValue("userId", saga.getUserId());
            command.setPayloadValue("verificationType", "BASIC");
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_VALIDATE) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("userId", saga.getUserId());
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_CHECK_BALANCE) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("userId", saga.getUserId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("currency", saga.getCurrency());
            command.setPayloadValue("paymentMethodId", saga.getPaymentMethodId());
        }
        if (nextStep == WithdrawalSagaStep.PAYMENT_METHOD_VALIDATE) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("paymentMethodId", saga.getPaymentMethodId());
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("userId", saga.getUserId());
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("userId", saga.getUserId());
            command.setPayloadValue("paymentMethodId", saga.getPaymentMethodId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("currency", saga.getCurrency());
            command.setPayloadValue("description", saga.getStepData().get("description"));
        }
        if (nextStep == WithdrawalSagaStep.PAYMENT_PROCESS_WITHDRAWAL) {
            command.setTargetService("PAYMENT_SERVICE");
            command.setPayloadValue("paymentMethodId", saga.getPaymentMethodId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("currency", saga.getCurrency());
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("transactionId", saga.getTransactionId());
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_UPDATE_TRANSACTION_STATUS) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("transactionId", saga.getTransactionId());
            command.setPayloadValue("status", "COMPLETED");
            command.setPayloadValue("paymentReference", saga.getPaymentProcessorTransactionId());
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE) {
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("transactionId", saga.getTransactionId());
        }
        if (nextStep == WithdrawalSagaStep.COMPLETE_SAGA) {
            command.setTargetService(null);

            saga.setEndTime(Instant.now());
        }
        if (nextStep == WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE) {
            command.setIsCompensation(true);
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("accountId", saga.getAccountId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("transactionId", saga.getTransactionId());
            command.setPayloadValue("reason", saga.getFailureReason());

            saga.getSagaEvents().add(SagaEvent.of("COMPENSATION_STARTED", "Starting compensation process"));
            saga.getSagaEvents().add(SagaEvent.of("COMPENSATION_STEP", "Starting compensation with step: " + nextStep));
        }
        if (nextStep == WithdrawalSagaStep.PAYMENT_REVERSE_WITHDRAWAL) {
            command.setIsCompensation(true);
            command.setTargetService("PAYMENT_SERVICE");
            command.setPayloadValue("paymentReference", saga.getPaymentProcessorTransactionId());
            command.setPayloadValue("amount", saga.getAmount());
            command.setPayloadValue("reason", saga.getFailureReason());
            command.setPayloadValue("transactionId", saga.getTransactionId());
        }
        if (nextStep == WithdrawalSagaStep.MARK_TRANSACTION_FAILED) {
            command.setIsCompensation(true);
            command.setTargetService("ACCOUNT_SERVICE");
            command.setPayloadValue("transactionId", saga.getTransactionId());
            command.setPayloadValue("failureReason", saga.getFailureReason());
            command.setPayloadValue("errorCode", "SAGA_FAILURE");
        }
        saga.getCompletedSteps().add(command.getIsCompensation() ? "COMP_" + saga.getCurrentStep().name() : saga.getCurrentStep().name());
        command.setMetadataValue("retryCount", String.valueOf(saga.getRetryCount()));
        String targetTopic = getTopicForCommandType(command.getTargetService());

        if (nextStep != WithdrawalSagaStep.COMPLETE_SAGA && nextStep != WithdrawalSagaStep.COMPLETE_COMPENSATION) {
            saga.setStatus(!command.getIsCompensation() ? SagaStatus.IN_PROGRESS : SagaStatus.COMPENSATING);
            saga.getSagaEvents().add(SagaEvent.of("STEP_CHANGED", "Moving to step: " + nextStep.getDescription()));
            saga.setCurrentStep(nextStep);
            saga.setLastUpdatedTime(Instant.now());
            saga.setCurrentStepStartTime(Instant.now());

            kafkaMessagePublisher.publishCommand(command, targetTopic);
            log.info("Published command [{}] for saga [{}] to topic: {}",
                    command.getType(), saga.getSagaId(), targetTopic);
        }
        else if (nextStep == WithdrawalSagaStep.COMPLETE_SAGA) {
            saga.setStatus(SagaStatus.COMPLETED);
            saga.getSagaEvents().add(SagaEvent.of("SAGA_COMPLETED","Withdrawal saga completed successfully"));
        }
        else {
            saga.setStatus(SagaStatus.COMPENSATION_COMPLETED);
            saga.getSagaEvents().add(SagaEvent.of("COMPENSATION_COMPLETED", "Compensation process completed"));
        }

        withdrawalSagaRepository.save(saga);
    }

    public void handleEventMessage(EventMessage event) {
        String sagaId = event.getSagaId();

        log.debug("Handling event [{}] for saga: {}", event.getType(), sagaId);

        // Find the saga
        Optional<WithdrawalSagaState> optionalSaga = withdrawalSagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Received event for unknown saga: {}", sagaId);
            return;
        }

        WithdrawalSagaState saga = optionalSaga.get();

        // Log saga state for debugging
        log.debug("Current saga state: id={}, status={}, currentStep={}",
                saga.getSagaId(), saga.getStatus(),
                saga.getCurrentStep());

        // Record processing to ensure idempotency
        if (idempotencyService.isProcessed(event)) {
            log.info("Event [{}] for saga [{}] has already been processed", event.getType(), sagaId);
            return;
        }

        // Check if this is a response to the current step
        boolean matchesCurrentStep = isEventForCurrentStep(saga, event);
        log.debug("Event matches current step: {}", matchesCurrentStep);

        if (!matchesCurrentStep) {
            log.warn("Received event [{}] for saga [{}] but doesn't match current step [{}]",
                    event.getType(), sagaId, saga.getCurrentStep());

            // Record the event processing anyway
            Map<String, Object> result = new HashMap<>();
            result.put("ignored", true);
            result.put("reason", "Event does not match current step");
            idempotencyService.recordProcessing(event, result);

            return;
        }

        // Process the event based on success/failure
        if (Boolean.TRUE.equals(event.getSuccess())) {
            log.debug("Processing success event for step: {}", saga.getCurrentStep());
            if (event.getType().equals(EventType.WITHDRAWAL_TRANSACTION_CREATED.name())) {
                saga.setTransactionId(event.getPayloadValue("transactionId"));
            }
            if (event.getType().equals(EventType.WITHDRAWAL_PAYMENT_PROCESSED.name())) {
                saga.setPaymentProcessorTransactionId(event.getPayloadValue("paymentReference"));
            }

            withdrawalSagaRepository.save(saga);

            nextSagaStep(saga);
        } else {
            try {
                log.debug("Processing failure event for step: {}", saga.getCurrentStep());
                log.warn("Processing failure event [{}] for saga [{}]: {}",
                        event.getType(), saga.getSagaId(), event.getErrorMessage());

                WithdrawalSagaStep currentStep = saga.getCurrentStep();
                saga.getSagaEvents().add(SagaEvent.of("STEP_FAILED", "Step " +currentStep.name() + " failed: " + event.getErrorMessage()));

                if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_VALIDATE)
                    || currentStep.equals(WithdrawalSagaStep.ACCOUNT_CHECK_BALANCE)
                    || currentStep.equals(WithdrawalSagaStep.PAYMENT_METHOD_VALIDATE)
                    || currentStep.equals(WithdrawalSagaStep.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION)
                    || currentStep.equals(WithdrawalSagaStep.USER_VERIFY_IDENTITY)) {

                    saga.setEndTime(Instant.now());
                    saga.getSagaEvents().add(SagaEvent.of("SAGA_TERMINATED", "Saga terminated due to validation failure"));

                    withdrawalSagaRepository.save(saga);
                }
                else {
                    saga.setCurrentStep(saga.getCompletedSteps().contains(WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE.name())
                                        ? WithdrawalSagaStep.START_COMPENSATION
                                        : WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE);

                    withdrawalSagaRepository.save(saga);

                    nextSagaStep(saga);
                }
            } catch (Exception e) {
                log.error("Error processing failure event: {}", e.getMessage(), e);

                // Ensure we still record the event as processed even if there's an error
                Map<String, Object> result = new HashMap<>();
                result.put("error", true);
                result.put("errorMessage", e.getMessage());
                idempotencyService.recordProcessing(event, result);

                // Rethrow to let the caller handle it
                throw e;
            }
        }

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("newStatus", saga.getStatus().name());
        result.put("newStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");
        idempotencyService.recordProcessing(event, result);

        // Log final state after processing
        log.debug("After event processing, saga state: id={}, status={}, currentStep={}",
                saga.getSagaId(), saga.getStatus(), saga.getCurrentStep());
    }

/*===================================================== PRIVATE FUNCTIONS =============================================================================*/
    private WithdrawalSagaStep getNextStep(WithdrawalSagaStep currentStep) {
        if (currentStep == WithdrawalSagaStep.START) {
            return WithdrawalSagaStep.USER_VERIFY_IDENTITY;
        }
        if (currentStep.equals(WithdrawalSagaStep.USER_VERIFY_IDENTITY)) {
            return WithdrawalSagaStep.ACCOUNT_VALIDATE;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_VALIDATE)) {
            return WithdrawalSagaStep.ACCOUNT_CHECK_BALANCE;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_CHECK_BALANCE)) {
            return WithdrawalSagaStep.PAYMENT_METHOD_VALIDATE;
        }
        if (currentStep.equals(WithdrawalSagaStep.PAYMENT_METHOD_VALIDATE)) {
            return WithdrawalSagaStep.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION)) {
            return WithdrawalSagaStep.PAYMENT_PROCESS_WITHDRAWAL;
        }
        if (currentStep.equals(WithdrawalSagaStep.PAYMENT_PROCESS_WITHDRAWAL)) {
            return WithdrawalSagaStep.ACCOUNT_UPDATE_TRANSACTION_STATUS;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_UPDATE_TRANSACTION_STATUS)) {
            return WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE)) {
            return WithdrawalSagaStep.COMPLETE_SAGA;
        }
        if (currentStep.equals(WithdrawalSagaStep.START_COMPENSATION)) {
            return WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE;
        }
        if (currentStep.equals(WithdrawalSagaStep.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE)) {
            return WithdrawalSagaStep.PAYMENT_REVERSE_WITHDRAWAL;
        }
        if (currentStep.equals(WithdrawalSagaStep.PAYMENT_REVERSE_WITHDRAWAL)) {
            return WithdrawalSagaStep.MARK_TRANSACTION_FAILED;
        }
        return WithdrawalSagaStep.COMPLETE_COMPENSATION;
    }

    private String getTopicForCommandType(String targetService) {
        switch (targetService) {
            case "USER_SERVICE":
                return "user.commands.verify";
            case "ACCOUNT_SERVICE":
                return "account.commands.deposit";
            case "PAYMENT_SERVICE":
                return "payment.commands.process.deposit";
            default:
                log.warn("Unknown service: {}", targetService);
                return "account.commands.deposit";
        }
    }

    /**
     * Check if this event is a response to the current saga step
     */
    private boolean isEventForCurrentStep(WithdrawalSagaState saga, EventMessage event) {
        if (saga.getCurrentStep() == null) {
            return false;
        }

        try {
            EventType eventType = EventType.valueOf(event.getType());
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();

            // Get the command type associated with this event type
            CommandType eventCommandType = eventType.getAssociatedCommandType();

            // This is the key check - does this event correspond to the current step's command?
            boolean matchingCommandType = eventCommandType == expectedCommandType;

            // Check if the step ID matches (if provided)
            boolean matchingStepId = event.getStepId() == null ||
                    event.getStepId().equals(saga.getCurrentStep().getStepNumber());

            // Log details in case of mismatch for debugging
            if (!matchingCommandType) {
                log.debug("Event command type mismatch: event={}, expected={}",
                        eventCommandType, expectedCommandType);
            }

            if (!matchingStepId) {
                log.debug("Event step ID mismatch: event={}, current={}",
                        event.getStepId(), saga.getCurrentStep().getStepNumber());
            }

            return matchingCommandType && matchingStepId;

        } catch (Exception e) {
            log.error("Error checking if event matches current step: {}", e.getMessage(), e);
            return false;
        }
    }
}
