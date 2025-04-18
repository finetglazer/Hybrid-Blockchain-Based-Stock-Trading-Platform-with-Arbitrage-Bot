package com.project.kafkamessagemodels.model.enums;

import lombok.Getter;

/**
 * Enum defining all command types used in the system
 */
@Getter
public enum CommandType {
    START("Start"),

    // User Service Commands
    USER_VERIFY_IDENTITY("Verify user identity"),

    // Account Service Commands
    ACCOUNT_VALIDATE("Validate account status"), // Add this new command
    PAYMENT_METHOD_VALIDATE("Validate payment method"),
    ACCOUNT_CHECK_BALANCE("Check available balance"),
    ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION("Create pending deposit transaction"),
    ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION("Create pending withdrawal transaction"),
    ACCOUNT_UPDATE_TRANSACTION_STATUS("Update transaction status"),
    ACCOUNT_DEPOSIT_UPDATE_BALANCE("Update account balance"),
    ACCOUNT_WITHDRAWAL_UPDATE_BALANCE("Update account balance"),

    // Payment Processor Commands
    PAYMENT_PROCESS_DEPOSIT("Process deposit payment"),
    PAYMENT_PROCESS_WITHDRAWAL("Process withdrawal payment"),

    // Compensation Commands
    START_COMPENSATION("Start compensation"),
    ACCOUNT_MARK_TRANSACTION_FAILED("Mark transaction as failed"),
    PAYMENT_REVERSE_DEPOSIT("Reverse deposit payment"),
    PAYMENT_REVERSE_WITHDRAWAL("Reverse withdrawal payment"),
    ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE("Reverse balance update"),
    ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE("Reverse balance update");

    private final String description;

    CommandType(String description) {
        this.description = description;
    }

    public String getValue() {
        return this.name();
    }

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
