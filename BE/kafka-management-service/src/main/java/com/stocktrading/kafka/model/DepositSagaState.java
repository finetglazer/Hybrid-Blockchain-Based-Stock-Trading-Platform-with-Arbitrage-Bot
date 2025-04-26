package com.stocktrading.kafka.model;


import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.stocktrading.kafka.model.enums.DepositSagaStep;
import com.stocktrading.kafka.model.enums.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the current state of a deposit saga
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "deposit_sagas")
public class DepositSagaState {
    @Id  // This should map to MongoDB's _id field
    private String id; // Rename from sagaId to id

    // Add a separate field if you want to maintain sagaId semantics
    @Indexed(unique = true)
    private String sagaId;
    
    // Business data
    private String userId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
    private String transactionId;
    private String paymentProcessorTransactionId;
    
    // Saga execution state
    private DepositSagaStep currentStep;
    private SagaStatus status;
    private List<String> completedSteps;
    private List<SagaEvent> sagaEvents;
    private String failureReason;
    private Map<String, Object> stepData;
    
    // Timing information
    private Instant startTime;
    private Instant endTime;
    private Instant lastUpdatedTime;
    private Instant currentStepStartTime;
    
    // Retry information
    private int retryCount;
    private int maxRetries;
    
    /**
     * Initialize a new deposit saga
     */
    public static DepositSagaState initiate(String sagaId, String userId, String accountId, 
                                        BigDecimal amount, String currency, 
                                        String paymentMethodId, int maxRetries) {


        return DepositSagaState.builder()
                .id(sagaId)       // Set the MongoDB _id field
                .sagaId(sagaId)   // Set the business sagaId field
                .userId(userId)
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .paymentMethodId(paymentMethodId)
                .currentStep(DepositSagaStep.VERIFY_USER_IDENTITY)
                .status(SagaStatus.STARTED)
                .completedSteps(new ArrayList<>())
                .sagaEvents(new ArrayList<>())
                .stepData(new HashMap<>())
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(maxRetries)
                .build();
    }
    
    /**
     * Move to the next step in the saga
     */
    public void moveToNextStep() {
        // Add current step to completed steps
        if (currentStep != null && !currentStep.isCompensationStep()) {
            completedSteps.add(currentStep.name());
        }
        
        DepositSagaStep nextStep = currentStep.getNextStep();
        currentStep = nextStep;
        currentStepStartTime = Instant.now();
        
        if (nextStep == DepositSagaStep.COMPLETE_SAGA) {
            status = SagaStatus.COMPLETED;
            endTime = Instant.now();
            addEvent("SAGA_COMPLETED", "Deposit saga completed successfully");
        } else {
            status = SagaStatus.IN_PROGRESS;
            addEvent("STEP_CHANGED", "Moving to step: " + nextStep.getDescription());
        }
        
        lastUpdatedTime = Instant.now();
    }
    
    /**
     * Handle a step failure
     */
    public void handleFailure(String reason, String stepName) {
        failureReason = reason;
        status = SagaStatus.FAILED;
        addEvent("STEP_FAILED", "Step " + stepName + " failed: " + reason);
        lastUpdatedTime = Instant.now();
    }

    /**
     * Start compensation process with proper reverse order
     */
    public void startCompensation() {
        status = SagaStatus.COMPENSATING;
        addEvent("COMPENSATION_STARTED", "Starting compensation process");

        // Check which steps have been completed to determine correct compensation chain
        boolean balanceUpdated = completedSteps.contains(DepositSagaStep.UPDATE_BALANCE.name());
        boolean transactionUpdated = completedSteps.contains(DepositSagaStep.UPDATE_TRANSACTION_STATUS.name());
        boolean paymentProcessed = completedSteps.contains(DepositSagaStep.PROCESS_PAYMENT.name());

        // Use the helper method to determine first compensation step
        currentStep = DepositSagaStep.determineFirstCompensationStep(
                balanceUpdated,
                transactionUpdated,
                paymentProcessed
        );

        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent("COMPENSATION_STEP", "Starting compensation with step: " + currentStep.getDescription());
    }
    /**
     * Complete compensation process
     */
    public void completeCompensation() {
        status = SagaStatus.COMPENSATION_COMPLETED;
        currentStep = DepositSagaStep.COMPLETE_SAGA; // Explicitly set current step to COMPLETE_SAGA
        endTime = Instant.now();
        addEvent("COMPENSATION_COMPLETED", "Compensation process completed");
        lastUpdatedTime = Instant.now();
    }
    
    /**
     * Add an event to the saga history
     */
    public void addEvent(String type, String description) {
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
        sagaEvents.add(SagaEvent.of(type, description));
    }
    
    /**
     * Store data for the current step
     */
    public void storeStepData(String key, Object value) {
        if (stepData == null) {
            stepData = new HashMap<>();
        }
        stepData.put(key, value);
    }
    
    /**
     * Get stored step data
     */
    @SuppressWarnings("unchecked")
    public <T> T getStepData(String key) {
        if (stepData == null) {
            return null;
        }
        return (T) stepData.get(key);
    }
    
    /**
     * Check if the current step has timed out
     */
    public boolean isCurrentStepTimedOut(Duration timeout) {
        if (currentStepStartTime == null) {
            return false;
        }
        return Duration.between(currentStepStartTime, Instant.now()).compareTo(timeout) > 0;
    }
    
    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        retryCount++;
        lastUpdatedTime = Instant.now();
    }
    
    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= maxRetries;
    }
    
    /**
     * Create command based on current step
     */
    public CommandMessage createCommandForCurrentStep() {
        CommandType commandType = currentStep.getCommandType();
        if (commandType == null) {
            return null;
        }
        
        CommandMessage command = new CommandMessage();
        command.setMessageId(null); // Will be auto-generated
        command.setSagaId(sagaId);
        command.setStepId(currentStep.getStepNumber());
        command.setType(commandType.name());
        command.setSourceService("SAGA_ORCHESTRATOR");
        command.setTargetService(commandType.getTargetService());
        command.setIsCompensation(currentStep.isCompensationStep());
        command.setTimestamp(Instant.now());
        
        // Add payload based on command type
        switch (commandType) {
            case USER_VERIFY_IDENTITY:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("verificationType", "BASIC");
                break;

            case ACCOUNT_VALIDATE: // Add this new case
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("userId", userId);
                break;
                
            case PAYMENT_METHOD_VALIDATE:
                command.setPayloadValue("paymentMethodId", paymentMethodId);
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("currency", currency);
                break;
                
            case ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("currency", currency);
                command.setPayloadValue("description", "Deposit funds");
                command.setPayloadValue("paymentMethodId", paymentMethodId);
                break;
                
            case PAYMENT_PROCESS_DEPOSIT:
                command.setPayloadValue("paymentMethodId", paymentMethodId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("currency", currency);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("transactionId", transactionId);
                break;
                
            case ACCOUNT_UPDATE_TRANSACTION_STATUS:
                command.setPayloadValue("transactionId", transactionId);
                command.setPayloadValue("status", "COMPLETED");
                command.setPayloadValue("paymentReference", paymentProcessorTransactionId);
                break;
                
            case ACCOUNT_DEPOSIT_UPDATE_BALANCE:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("transactionId", transactionId);
                break;
                
            case ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("transactionId", transactionId);
                command.setPayloadValue("reason", failureReason);
                break;
                
            case PAYMENT_REVERSE_DEPOSIT:
                command.setPayloadValue("paymentReference", paymentProcessorTransactionId);
                command.setPayloadValue("amount", amount);
                command.setPayloadValue("reason", failureReason);
                command.setPayloadValue("transactionId", transactionId);
                break;
                
            case ACCOUNT_MARK_TRANSACTION_FAILED:
                command.setPayloadValue("transactionId", transactionId);
                command.setPayloadValue("failureReason", failureReason);
                command.setPayloadValue("errorCode", "SAGA_FAILURE");
                break;
        }
        
        command.setMetadataValue("retryCount", String.valueOf(retryCount));
        return command;
    }
}
