package com.project.kafkamessagemodels.model.enums;

import com.project.kafkamessagemodels.model.enums.CommandType;

/**
 * Enum defining all event types used in the system
 */
public enum EventType {
    // User Service Events
    USER_IDENTITY_VERIFIED("User identity verified"),
    USER_VERIFICATION_FAILED("User verification failed"),

    // Account Service Events
    ACCOUNT_VALIDATED("Account validated"),
    ACCOUNT_VALIDATION_FAILED("Account validation failed"),
    PAYMENT_METHOD_VALID("Payment method validated"),
    PAYMENT_METHOD_INVALID("Payment method invalid"),
    TRANSACTION_CREATED("Transaction created"),
    TRANSACTION_CREATION_FAILED("Transaction creation failed"),
    TRANSACTION_STATUS_UPDATED("Transaction status updated"),
    TRANSACTION_UPDATE_FAILED("Transaction status update failed"),
    BALANCE_UPDATED("Balance updated"),
    BALANCE_UPDATE_FAILED("Balance update failed"),

    // Payment Processor Events
    PAYMENT_PROCESSED("Payment processed"),
    PAYMENT_FAILED("Payment processing failed"),

    // Compensation Event Responses
    PAYMENT_REVERSAL_COMPLETED("Payment reversal completed"),
    BALANCE_REVERSAL_COMPLETED("Balance reversal completed");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return this.name();
    }

    /**
     * Get the associated command type for this event (if applicable)
     */
    public CommandType getAssociatedCommandType() {
        switch (this) {
            case USER_IDENTITY_VERIFIED:
            case USER_VERIFICATION_FAILED:
                return CommandType.USER_VERIFY_IDENTITY;

            case ACCOUNT_VALIDATED:
            case ACCOUNT_VALIDATION_FAILED:
                return CommandType.ACCOUNT_VALIDATE;

            case PAYMENT_METHOD_VALID:
            case PAYMENT_METHOD_INVALID:
                return CommandType.PAYMENT_METHOD_VALIDATE;

            case TRANSACTION_CREATED:
            case TRANSACTION_CREATION_FAILED:
                return CommandType.ACCOUNT_CREATE_PENDING_TRANSACTION;

            case PAYMENT_PROCESSED:
            case PAYMENT_FAILED:
                return CommandType.PAYMENT_PROCESS_DEPOSIT;

            case TRANSACTION_STATUS_UPDATED:
            case TRANSACTION_UPDATE_FAILED:
                return CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS;

            case BALANCE_UPDATED:
            case BALANCE_UPDATE_FAILED:
                return CommandType.ACCOUNT_UPDATE_BALANCE;

            case PAYMENT_REVERSAL_COMPLETED:
                return CommandType.PAYMENT_REVERSE_DEPOSIT;

            case BALANCE_REVERSAL_COMPLETED:
                return CommandType.ACCOUNT_REVERSE_BALANCE_UPDATE;

            default:
                return null;
        }
    }
}