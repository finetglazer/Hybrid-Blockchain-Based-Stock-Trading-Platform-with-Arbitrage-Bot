package com.project.kafkamessagemodels.model.enums;

/**
 * Enum defining all command types used in the system
 */
public enum CommandType {
    // User Service Commands
    USER_VERIFY_IDENTITY("Verify user identity"),

    // Account Service Commands
    PAYMENT_METHOD_VALIDATE("Validate payment method"),
    ACCOUNT_CREATE_PENDING_TRANSACTION("Create pending transaction"),
    ACCOUNT_UPDATE_TRANSACTION_STATUS("Update transaction status"),
    ACCOUNT_UPDATE_BALANCE("Update account balance"),

    // Payment Processor Commands
    PAYMENT_PROCESS_DEPOSIT("Process deposit payment"),

    // Compensation Commands
    ACCOUNT_MARK_TRANSACTION_FAILED("Mark transaction as failed"),
    PAYMENT_REVERSE_DEPOSIT("Reverse deposit payment"),
    ACCOUNT_REVERSE_BALANCE_UPDATE("Reverse balance update");

    private final String description;

    CommandType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return this.name();
    }

    /**
     * Get the target service for a command type
     */
    /**
     * Get the target service for a command type
     */
    public String getTargetService() {
        // Special case for payment method validation which belongs to account service
        if (this == PAYMENT_METHOD_VALIDATE) {
            return "ACCOUNT_SERVICE";
        }

        // Otherwise use the prefix rule
        if (this.name().startsWith("USER_")) {
            return "USER_SERVICE";
        } else if (this.name().startsWith("PAYMENT_")) {
            return "PAYMENT_SERVICE";
        } else if (this.name().startsWith("ACCOUNT_")) {
            return "ACCOUNT_SERVICE";
        } else {
            return "UNKNOWN_SERVICE";
        }
    }
}