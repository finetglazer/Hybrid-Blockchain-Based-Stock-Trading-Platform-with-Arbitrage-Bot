package com.stocktrading.kafka.model.enums;

import com.project.kafkamessagemodels.model.enums.CommandType;

/**
 * Enum defining all steps in the deposit saga
 */
public enum DepositSagaStep {
    // Normal flow steps
    VERIFY_USER_IDENTITY(1, "Verify user identity", CommandType.USER_VERIFY_IDENTITY),
    VALIDATE_ACCOUNT(2, "Validate account status", CommandType.ACCOUNT_VALIDATE), // Add this new step
    VALIDATE_PAYMENT_METHOD(3, "Validate payment method", CommandType.PAYMENT_METHOD_VALIDATE), // Updated step number
    CREATE_PENDING_TRANSACTION(4, "Create pending transaction", CommandType.ACCOUNT_CREATE_PENDING_TRANSACTION), // Updated step number
    PROCESS_PAYMENT(5, "Process payment", CommandType.PAYMENT_PROCESS_DEPOSIT), // Updated step number
    UPDATE_TRANSACTION_STATUS(6, "Update transaction status", CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS), // Updated step number
    UPDATE_BALANCE(7, "Update balance", CommandType.ACCOUNT_UPDATE_BALANCE), // Updated step number
    COMPLETE_SAGA(8, "Complete saga", null), // Updated step number

    // Compensation steps
    REVERSE_BALANCE_UPDATE(101, "Reverse balance update", CommandType.ACCOUNT_REVERSE_BALANCE_UPDATE),
    REVERSE_PAYMENT(102, "Reverse payment", CommandType.PAYMENT_REVERSE_DEPOSIT),
    MARK_TRANSACTION_FAILED(103, "Mark transaction failed", CommandType.ACCOUNT_MARK_TRANSACTION_FAILED);
    
    private final int stepNumber;
    private final String description;
    private final CommandType commandType;
    
    DepositSagaStep(int stepNumber, String description, CommandType commandType) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.commandType = commandType;
    }
    
    public int getStepNumber() {
        return stepNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public CommandType getCommandType() {
        return commandType;
    }
    
    /**
     * Get next step in normal flow
     */
    public DepositSagaStep getNextStep() {
        switch (this) {
            case VERIFY_USER_IDENTITY:
                return VALIDATE_ACCOUNT; // Updated to go to account validation
            case VALIDATE_ACCOUNT:
                return VALIDATE_PAYMENT_METHOD; // New step goes to payment method validation
            case VALIDATE_PAYMENT_METHOD:
                return CREATE_PENDING_TRANSACTION;
            case CREATE_PENDING_TRANSACTION:
                return PROCESS_PAYMENT;
            case PROCESS_PAYMENT:
                return UPDATE_TRANSACTION_STATUS;
            case UPDATE_TRANSACTION_STATUS:
                return UPDATE_BALANCE;
            case UPDATE_BALANCE:
                return COMPLETE_SAGA;
            default:
                return null;
        }
    }
    
    /**
     * Get step by step number
     */
    public static DepositSagaStep getByStepNumber(int stepNumber) {
        for (DepositSagaStep step : DepositSagaStep.values()) {
            if (step.getStepNumber() == stepNumber) {
                return step;
            }
        }
        return null;
    }
    
    /**
     * Determine if this is a compensation step
     */
    public boolean isCompensationStep() {
        return stepNumber >= 100;
    }
}
