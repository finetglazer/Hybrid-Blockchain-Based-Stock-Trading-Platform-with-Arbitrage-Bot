package com.stocktrading.kafka.model.enums;

/**
 * Enum defining all steps in the deposit saga
 */
public enum DepositSagaStep {
    // Normal flow steps
    VERIFY_USER_IDENTITY(1, "Verify user identity", CommandType.USER_VERIFY_IDENTITY),
    VALIDATE_PAYMENT_METHOD(2, "Validate payment method", CommandType.PAYMENT_METHOD_VALIDATE),
    CREATE_PENDING_TRANSACTION(3, "Create pending transaction", CommandType.ACCOUNT_CREATE_PENDING_TRANSACTION),
    PROCESS_PAYMENT(4, "Process payment", CommandType.PAYMENT_PROCESS_DEPOSIT),
    UPDATE_TRANSACTION_STATUS(5, "Update transaction status", CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS),
    UPDATE_BALANCE(6, "Update balance", CommandType.ACCOUNT_UPDATE_BALANCE),
    COMPLETE_SAGA(7, "Complete saga", null),
    
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
                return VALIDATE_PAYMENT_METHOD;
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
