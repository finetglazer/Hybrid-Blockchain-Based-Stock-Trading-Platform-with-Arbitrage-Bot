package com.stocktrading.kafka.model.enums;

/**
 * Enum defining all event types used in the system
 */
public enum EventType {
    // User Service Events
    USER_IDENTITY_VERIFIED("User identity verified"),

    // Account Service Events
    PAYMENT_METHOD_VALID("Payment method validated"),
    TRANSACTION_CREATED("Transaction created"),
    TRANSACTION_STATUS_UPDATED("Transaction status updated"),
    BALANCE_UPDATED("Balance updated"),

    // Payment Processor Events
    PAYMENT_PROCESSED("Payment processed"),

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
                return CommandType.USER_VERIFY_IDENTITY;
            case PAYMENT_METHOD_VALID:
                return CommandType.PAYMENT_METHOD_VALIDATE;
            case TRANSACTION_CREATED:
                return CommandType.ACCOUNT_CREATE_PENDING_TRANSACTION;
            case PAYMENT_PROCESSED:
                return CommandType.PAYMENT_PROCESS_DEPOSIT;
            case TRANSACTION_STATUS_UPDATED:
                return CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS;
            case BALANCE_UPDATED:
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