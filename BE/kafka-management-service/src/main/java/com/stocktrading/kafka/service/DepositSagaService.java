package com.stocktrading.kafka.service;


import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.project.kafkamessagemodels.model.enums.EventType;
import com.stocktrading.kafka.model.DepositSagaState;


import com.stocktrading.kafka.model.enums.DepositSagaStep;

import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.model.enums.WithdrawalSagaStep;
import com.stocktrading.kafka.repository.DepositSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing the deposit saga workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepositSagaService {
    
    private final DepositSagaRepository depositSagaRepository;
    private final KafkaMessagePublisher messagePublisher;
    private final IdempotencyService idempotencyService;
    
    @Value("${saga.deposit.retry.max-attempts}")
    private int maxRetries;
    
    @Value("${saga.deposit.timeout.verify-identity}")
    private long verifyIdentityTimeout;
    
    @Value("${saga.deposit.timeout.validate-payment}")
    private long validatePaymentTimeout;
    
    @Value("${saga.deposit.timeout.create-transaction}")
    private long createTransactionTimeout;
    
    @Value("${saga.deposit.timeout.process-payment}")
    private long processPaymentTimeout;
    
    @Value("${saga.deposit.timeout.update-transaction}")
    private long updateTransactionTimeout;
    
    @Value("${saga.deposit.timeout.update-balance}")
    private long updateBalanceTimeout;

    @Value("${kafka.topics.user-commands.common:user.commands.common}")
    private String userCommonCommandsTopic;

    @Value("${kafka.topics.account-commands.common:user.commands.common}")
    private String accountCommonCommandsTopic;

    @Value("${kafka.topics.account-commands.deposit:user.commands.deposit}")
    private String accountDepositCommandsTopic;

    @Value("${kafka.topics.payment-commands.deposit:payment.commands.process}")
    private String paymentDepositCommandsTopic;

    /**
     * Start a new deposit saga
     */
    public DepositSagaState startSaga(String userId, String accountId, BigDecimal amount,
                                     String currency, String paymentMethodId) {
        String sagaId = UUID.randomUUID().toString();

        log.debug("Starting saga with sagaId: {}, amount: {} (type: {})",
                sagaId, amount, amount.getClass().getName());

        DepositSagaState saga = DepositSagaState.initiate(
                sagaId, userId, accountId, amount, currency, paymentMethodId, maxRetries);

        // Debug: Verify saga fields before saving
        log.debug("Saga before save: sagaId={}, amount={} (type={})",
                saga.getSagaId(), saga.getAmount(), saga.getAmount().getClass().getName());

        depositSagaRepository.save(saga);

        // Process the first step
        processNextStep(saga);

        return saga;
    }
    
    /**
     * Process the next step in the saga
     */
    public void processNextStep(DepositSagaState saga) {
        log.debug("Processing step [{}] for saga: {}", saga.getCurrentStep(), saga.getSagaId());
        
        CommandMessage command = saga.createCommandForCurrentStep();
        if (command == null) {
            // This can happen for the COMPLETE_SAGA step which doesn't have a command
            if (saga.getCurrentStep() == DepositSagaStep.COMPLETE_SAGA) {
                saga.moveToNextStep(); // This will mark the saga as COMPLETED
                depositSagaRepository.save(saga);
                log.info("Deposit saga [{}] completed successfully", saga.getSagaId());
            } else {
                log.warn("No command defined for step: {} in saga: {}", 
                    saga.getCurrentStep(), saga.getSagaId());
            }
            return;
        }
        
        // Initialize the command
        command.initialize();
        
        // Save the updated saga state
        depositSagaRepository.save(saga);
        
        // Determine the topic based on the command type
        String targetTopic = getTopicForCommandType(CommandType.valueOf(command.getType()));
        
        // Publish the command
        messagePublisher.publishCommand(command, targetTopic);
        
        log.info("Published command [{}] for saga [{}] to topic: {}", 
            command.getType(), saga.getSagaId(), targetTopic);
    }

    /**
     * Handle an event message response
     */
    public void handleEventMessage(EventMessage event) {
        String sagaId = event.getSagaId();

        log.debug("Handling event [{}] for saga: {}", event.getType(), sagaId);

        // Find the saga
        Optional<DepositSagaState> optionalSaga = depositSagaRepository.getDepositSagaStateBySagaId(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Received event for unknown saga: {}", sagaId);
            return;
        }

        DepositSagaState saga = optionalSaga.get();

        // Log saga state for debugging
        log.debug("Current saga state: id={}, status={}, currentStep={}, isCompensationStep={}",
                saga.getSagaId(), saga.getStatus(),
                saga.getCurrentStep(), saga.getCurrentStep().isCompensationStep());

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
            processSuccessEvent(saga, event);
        } else {
            try {
                log.debug("Processing failure event for step: {}", saga.getCurrentStep());
                processFailureEvent(saga, event);
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

    /**
     * Process a successful event
     */
    private void processSuccessEvent(DepositSagaState saga, EventMessage event) {
        log.info("Processing success event [{}] for saga [{}]", event.getType(), saga.getSagaId());

        // Update saga with event data based on event type
        updateSagaWithEventData(saga, event);

        // Check if this is a compensation event
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // For compensation steps, we need to handle them differently
            handleCompensationStepSuccess(saga);
        } else {
            // Normal flow - move to the next step
            saga.moveToNextStep();
        }

        // Save the updated saga
        depositSagaRepository.save(saga);

        // Process the next step if saga is still active
        if (saga.getStatus() == SagaStatus.IN_PROGRESS ||
                saga.getStatus() == SagaStatus.COMPENSATING) {
            processNextStep(saga);
        }
    }

    /**
     * Handle successful completion of a compensation step
     */
    private void handleCompensationStepSuccess(DepositSagaState saga) {
        // Get the current compensation step
        DepositSagaStep currentStep = saga.getCurrentStep();

        log.debug("Handling successful completion of compensation step: {}", currentStep);

        // Add to completed steps
        if (saga.getCompletedSteps() == null) {
            saga.setCompletedSteps(new ArrayList<>());
        }
        saga.getCompletedSteps().add("COMP_" + currentStep.name());

        // Get the next compensation step
        DepositSagaStep nextStep = currentStep.getNextCompensationStep();
        log.debug("Next compensation step: {}", nextStep);

        if (nextStep == DepositSagaStep.COMPLETE_SAGA) {
            // All compensation steps completed
            saga.completeCompensation();
            log.info("Compensation process completed for saga [{}]", saga.getSagaId());
        } else {
            // More compensation steps to execute
            saga.setCurrentStep(nextStep);
            saga.setCurrentStepStartTime(Instant.now());
            saga.setLastUpdatedTime(Instant.now());
            saga.addEvent("COMPENSATION_STEP", "Moving to compensation step: " + nextStep.getDescription());
            log.info("Moving to next compensation step [{}] for saga [{}]",
                    nextStep.name(), saga.getSagaId());
        }
    }
    
    /**
     * Process a failure event
     */
    private void processFailureEvent(DepositSagaState saga, EventMessage event) {
        log.warn("Processing failure event [{}] for saga [{}]: {}",
                event.getType(), saga.getSagaId(), event.getErrorMessage());

        // Update saga with failure reason
        String failureReason = event.getErrorMessage();
        if (failureReason == null) {
            failureReason = "Failed in step: " + saga.getCurrentStep().name();
        }

        saga.handleFailure(failureReason, saga.getCurrentStep().name());

        // Check if we need compensation
        if (!isValidationStep(saga.getCurrentStep())) {
            // Start compensation if it's a resource-modifying step
            saga.startCompensation();

            // Save the updated saga
            depositSagaRepository.save(saga);

            // Start the compensation process
            processNextStep(saga);
        } else {
            // For validation steps, just terminate without compensation
            saga.setEndTime(Instant.now());
            saga.addEvent("SAGA_TERMINATED", "Saga terminated due to validation failure");
            log.info("Saga terminated due to validation failure at step: {}", saga.getCurrentStep().name());

            // Save the terminated saga
            depositSagaRepository.save(saga);
        }
    }

    /**
     * Check if the current step is a validation step (no compensation needed)
     */
    private boolean isValidationStep(DepositSagaStep step) {
        return step == DepositSagaStep.VERIFY_USER_IDENTITY ||
                step == DepositSagaStep.VALIDATE_ACCOUNT ||
                step == DepositSagaStep.VALIDATE_PAYMENT_METHOD ||
                step == DepositSagaStep.CREATE_PENDING_TRANSACTION;
    }
    /**
     * Update the saga state with data from the event
     */
    private void updateSagaWithEventData(DepositSagaState saga, EventMessage event) {
        try {
            EventType eventType = EventType.valueOf(event.getType());
            
            switch (eventType) {
                case DEPOSIT_TRANSACTION_CREATED:
                    saga.setTransactionId(event.getPayloadValue("transactionId"));
                    break;
                    
                case DEPOSIT_PAYMENT_PROCESSED:
                    saga.setPaymentProcessorTransactionId(event.getPayloadValue("paymentReference"));
                    break;
                    
                // Store other event data as needed
                default:
                    // No specific data to extract for other event types
                    break;
            }
            
            // Store any additional event payload data for future reference
            for (Map.Entry<String, Object> entry : event.getPayload().entrySet()) {
                saga.storeStepData(event.getType() + "_" + entry.getKey(), entry.getValue());
            }
            
        } catch (Exception e) {
            log.error("Error updating saga with event data", e);
        }
    }
    

    /**
     * Check if this event is a response to the current saga step
     */
    private boolean isEventForCurrentStep(DepositSagaState saga, EventMessage event) {
        if (saga.getCurrentStep() == null) {
            return false;
        }

        // For compensation steps, we need special handling
        if (saga.getCurrentStep().isCompensationStep()) {
            return isEventForCompensationStep(saga, event);
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
    /**
     * Check if this event is a response to the current compensation step
     */
    private boolean isEventForCompensationStep(DepositSagaState saga, EventMessage event) {
        try {
            EventType eventType = EventType.valueOf(event.getType());
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();

            // Log detailed matching information for debugging
            log.debug("Compensation event matching: eventType={}, eventCommandType={}, expectedCommandType={}, stepId={}, currentStepNumber={}",
                    eventType, eventType.getAssociatedCommandType(), expectedCommandType,
                    event.getStepId(), saga.getCurrentStep().getStepNumber());

            // For compensation steps, use more flexible matching criteria
            // 1. The event type should match the expected command type
            boolean commandTypeMatches = eventType.getAssociatedCommandType() == expectedCommandType;

            // 2. If stepId is provided, it should match, but don't require it
            boolean stepIdMatches = event.getStepId() == null ||
                    event.getStepId().equals(saga.getCurrentStep().getStepNumber());

            // Return true if basic command type matching passes
            return commandTypeMatches && stepIdMatches;
        } catch (Exception e) {
            log.error("Error checking if event matches compensation step: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the appropriate Kafka topic for a command type
     */
    private String getTopicForCommandType(CommandType commandType) {
        String serviceName = commandType.getTargetService();

        switch (serviceName) {
            case "USER_SERVICE":
                return userCommonCommandsTopic;
            case "ACCOUNT_SERVICE":
                if (commandType.equals(WithdrawalSagaStep.ACCOUNT_VALIDATE.getCommandType())
                        || commandType.equals(WithdrawalSagaStep.ACCOUNT_CHECK_BALANCE.getCommandType())
                        || commandType.equals(WithdrawalSagaStep.PAYMENT_METHOD_VALIDATE.getCommandType())
                        || commandType.equals(WithdrawalSagaStep.ACCOUNT_UPDATE_TRANSACTION_STATUS.getCommandType())
                        || commandType.equals(WithdrawalSagaStep.ACCOUNT_MARK_TRANSACTION_FAILED.getCommandType())) {

                    return accountCommonCommandsTopic;
                }
                return accountDepositCommandsTopic;
            case "PAYMENT_SERVICE":
                return paymentDepositCommandsTopic;
        }
        log.warn("Unsupported command type: {}", commandType);
        return accountDepositCommandsTopic;
    }
    
    /**
     * Check for timed-out steps and handle them
     */
    public void checkForTimeouts() {
        log.debug("Checking for timed-out saga steps");
        
        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS);
        
        // Find sagas that might have timed out
        Instant cutoffTime = Instant.now().minus(Duration.ofMillis(Math.max(
            verifyIdentityTimeout, Math.max(
                validatePaymentTimeout, Math.max(
                    createTransactionTimeout, Math.max(
                        processPaymentTimeout, Math.max(
                            updateTransactionTimeout, updateBalanceTimeout
                        )
                    )
                )
            )
        )));
        
        List<DepositSagaState> potentiallyTimedOutSagas = 
            depositSagaRepository.findPotentiallyTimedOutSagas(activeStatuses, cutoffTime);
        
        for (DepositSagaState saga : potentiallyTimedOutSagas) {
            handlePotentialTimeout(saga);
        }
    }
    
    /**
     * Handle a potentially timed-out saga
     */
    private void handlePotentialTimeout(DepositSagaState saga) {
        if (saga.getCurrentStep() == null) {
            return;
        }
        
        Duration timeout = getTimeoutForStep(saga.getCurrentStep());
        
        if (saga.isCurrentStepTimedOut(timeout)) {
            log.warn("Step [{}] has timed out for saga: {}", saga.getCurrentStep(), saga.getSagaId());
            
            // Check if we can retry
            if (saga.getRetryCount() < saga.getMaxRetries()) {
                // Increment retry count
                saga.incrementRetryCount();
                saga.addEvent("RETRY", "Retrying step " + saga.getCurrentStep() + " after timeout");
                
                // Save and retry the step
                depositSagaRepository.save(saga);
                processNextStep(saga);
                
            } else {
                // We've exceeded retries, start compensation
                saga.handleFailure("Step timed out after " + saga.getMaxRetries() + " retries", 
                    saga.getCurrentStep().name());
                saga.startCompensation();
                
                // Save and start compensation
                depositSagaRepository.save(saga);
                processNextStep(saga);
            }
        }
    }
    
    /**
     * Get the timeout duration for a specific step
     */
    private Duration getTimeoutForStep(DepositSagaStep step) {
        switch (step) {
            case VERIFY_USER_IDENTITY:
                return Duration.ofMillis(verifyIdentityTimeout);
            case VALIDATE_PAYMENT_METHOD:
                return Duration.ofMillis(validatePaymentTimeout);
            case CREATE_PENDING_TRANSACTION:
                return Duration.ofMillis(createTransactionTimeout);
            case PROCESS_PAYMENT:
                return Duration.ofMillis(processPaymentTimeout);
            case UPDATE_TRANSACTION_STATUS:
                return Duration.ofMillis(updateTransactionTimeout);
            case UPDATE_BALANCE:
                return Duration.ofMillis(updateBalanceTimeout);
            default:
                // For compensation steps, use a generous timeout
                return Duration.ofMillis(processPaymentTimeout); 
        }
    }
    
    /**
     * Find a saga by ID
     */
    public Optional<DepositSagaState> findById(String sagaId) {
        return depositSagaRepository.findById(sagaId);
    }
    
    /**
     * Find all active sagas
     */
    public List<DepositSagaState> findActiveSagas() {
        List<SagaStatus> activeStatuses = Arrays.asList(
            SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
        return depositSagaRepository.findByStatusIn(activeStatuses);
    }
}
