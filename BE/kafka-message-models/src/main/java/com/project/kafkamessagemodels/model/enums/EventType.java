package com.project.kafkamessagemodels.model.enums;

import lombok.Getter;

/**
 * Enum defining all event types used in the system
 */
@Getter
public enum EventType {
    // User Service Events
    USER_IDENTITY_VERIFIED("User identity verified"),
    USER_VERIFICATION_FAILED("User verification failed"),

    // Account Service Events
    ACCOUNT_VALIDATED("Account validated"),
    ACCOUNT_VALIDATION_FAILED("Account validation failed"),
    PAYMENT_METHOD_VALID("Payment method validated"),
    PAYMENT_METHOD_INVALID("Payment method invalid"),
    BALANCE_VALID("Balance valid"),
    BALANCE_VALIDATION_ERROR("Balance validation error"),
    DEPOSIT_TRANSACTION_CREATED("Transaction created"),
    DEPOSIT_TRANSACTION_CREATION_FAILED("Transaction creation failed"),
    WITHDRAWAL_TRANSACTION_CREATED("Transaction created"),
    WITHDRAWAL_TRANSACTION_CREATION_FAILED("Transaction creation failed"),TRANSACTION_STATUS_UPDATED("Transaction status updated"),
    TRANSACTION_UPDATE_FAILED("Transaction status update failed"),
    DEPOSIT_BALANCE_UPDATED("Balance updated"),
    DEPOSIT_BALANCE_UPDATE_FAILED("Balance update failed"),
    WITHDRAWAL_BALANCE_UPDATED("Balance updated"),
    WITHDRAWAL_BALANCE_UPDATE_FAILED("Balance update failed"),


    // Payment Processor Events
    DEPOSIT_PAYMENT_PROCESSED("Payment processed"),
    DEPOSIT_PAYMENT_FAILED("Payment processing failed"),
    WITHDRAWAL_PAYMENT_PROCESSED("Payment processed"),
    WITHDRAWAL_PAYMENT_FAILED("Payment processing failed"),

    // Compensation Event Responses
    DEPOSIT_PAYMENT_REVERSAL_COMPLETED("Payment reversal completed"),
    DEPOSIT_PAYMENT_REVERSAL_FAILED("Payment reversal failed"),
    WITHDRAWAL_PAYMENT_REVERSAL_COMPLETED("Payment reversal completed"),
    WITHDRAWAL_PAYMENT_REVERSAL_FAILED("Payment reversal failed"),
    DEPOSIT_BALANCE_REVERSAL_COMPLETED("Balance reversal completed"),
    DEPOSIT_BALANCE_REVERSAL_FAILED("Balance reversal failed"),
    WITHDRAWAL_BALANCE_REVERSAL_COMPLETED("Balance reversal completed"),
    WITHDRAWAL_BALANCE_REVERSAL_FAILED("Balance reversal failed"),
    TRANSACTION_MARKED_FAILED("Transaction marked as failed"),
    TRANSACTION_MARK_FAILED_ERROR("Error marking transaction as failed");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getValue() {
        return this.name();
    }

    /**
     * Get the associated command type for this event (if applicable)
     */
    public CommandType getAssociatedCommandType() {
        return switch (this) {
            case USER_IDENTITY_VERIFIED, USER_VERIFICATION_FAILED -> CommandType.USER_VERIFY_IDENTITY;
            case ACCOUNT_VALIDATED, ACCOUNT_VALIDATION_FAILED -> CommandType.ACCOUNT_VALIDATE;
            case PAYMENT_METHOD_VALID, PAYMENT_METHOD_INVALID -> CommandType.PAYMENT_METHOD_VALIDATE;
            case BALANCE_VALID, BALANCE_VALIDATION_ERROR -> CommandType.ACCOUNT_CHECK_BALANCE;
            case DEPOSIT_TRANSACTION_CREATED, DEPOSIT_TRANSACTION_CREATION_FAILED ->
                    CommandType.ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION;
            case WITHDRAWAL_TRANSACTION_CREATED, WITHDRAWAL_TRANSACTION_CREATION_FAILED ->
                    CommandType.ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION;
            case DEPOSIT_PAYMENT_PROCESSED, DEPOSIT_PAYMENT_FAILED -> CommandType.PAYMENT_PROCESS_DEPOSIT;
            case WITHDRAWAL_PAYMENT_PROCESSED, WITHDRAWAL_PAYMENT_FAILED -> CommandType.PAYMENT_PROCESS_WITHDRAWAL;
            case TRANSACTION_STATUS_UPDATED, TRANSACTION_UPDATE_FAILED -> CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS;
            case DEPOSIT_BALANCE_UPDATED, DEPOSIT_BALANCE_UPDATE_FAILED -> CommandType.ACCOUNT_DEPOSIT_UPDATE_BALANCE;
            case DEPOSIT_PAYMENT_REVERSAL_COMPLETED, DEPOSIT_PAYMENT_REVERSAL_FAILED -> CommandType.PAYMENT_REVERSE_DEPOSIT;
            case DEPOSIT_BALANCE_REVERSAL_COMPLETED, DEPOSIT_BALANCE_REVERSAL_FAILED ->
                    CommandType.ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE;
            case WITHDRAWAL_BALANCE_UPDATED, WITHDRAWAL_BALANCE_UPDATE_FAILED -> CommandType.ACCOUNT_WITHDRAWAL_UPDATE_BALANCE;
            case WITHDRAWAL_PAYMENT_REVERSAL_COMPLETED, WITHDRAWAL_PAYMENT_REVERSAL_FAILED -> CommandType.PAYMENT_REVERSE_WITHDRAWAL;
            case WITHDRAWAL_BALANCE_REVERSAL_COMPLETED, WITHDRAWAL_BALANCE_REVERSAL_FAILED ->
                    CommandType.ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE;
            case TRANSACTION_MARKED_FAILED, TRANSACTION_MARK_FAILED_ERROR ->
                    CommandType.ACCOUNT_MARK_TRANSACTION_FAILED;
        };
    }
}
