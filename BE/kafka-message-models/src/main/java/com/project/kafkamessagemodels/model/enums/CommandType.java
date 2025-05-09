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
    USER_VERIFY_TRADING_PERMISSIONS("Verify user trading permissions"),

    // Account Service Commands
    ACCOUNT_VALIDATE("Validate account status"),
    ACCOUNT_VERIFY_STATUS("Verify account status"),
    ACCOUNT_RESERVE_FUNDS("Reserve funds for order"),
    ACCOUNT_SETTLE_TRANSACTION("Settle order transaction"),
    ACCOUNT_RELEASE_FUNDS("Release reserved funds"),
    PAYMENT_METHOD_VALIDATE("Validate payment method"),
    ACCOUNT_CHECK_BALANCE("Check available balance"),
    ACCOUNT_CREATE_DEPOSIT_PENDING_TRANSACTION("Create pending deposit transaction"),
    ACCOUNT_CREATE_WITHDRAWAL_PENDING_TRANSACTION("Create pending withdrawal transaction"),
    ACCOUNT_UPDATE_TRANSACTION_STATUS("Update transaction status"),
    ACCOUNT_DEPOSIT_UPDATE_BALANCE("Update account balance"),
    ACCOUNT_WITHDRAWAL_UPDATE_BALANCE("Update account balance"),

    // Order Service Commands
    ORDER_CREATE("Create new order"),
    ORDER_UPDATE_VALIDATED("Update order to validated status"),
    ORDER_UPDATE_EXECUTED("Update order to executed status"),
    ORDER_UPDATE_COMPLETED("Update order to completed status"),
    ORDER_CANCEL("Cancel an order"),

    // Market Data Service Commands
    MARKET_VALIDATE_STOCK("Validate stock exists"),
    MARKET_GET_PRICE("Get current market price"),

    // Portfolio Service Commands
    PORTFOLIO_UPDATE_POSITIONS("Update portfolio positions"),
    PORTFOLIO_REMOVE_POSITIONS("Remove positions from portfolio"),
    PORTFOLIO_VERIFY_SHARES("Verify sufficient shares in portfolio"),
    PORTFOLIO_RESERVE_SHARES("Reserve shares for sale"),
    PORTFOLIO_RELEASE_SHARES("Release reserved shares"),
    PORTFOLIO_RESTORE_POSITIONS("Restore shares to portfolio"),

    // Mock Brokerage Service Commands
    BROKER_EXECUTE_ORDER("Execute order in market"),
    BROKER_CANCEL_ORDER("Cancel order with broker"),

    // Payment Processor Commands
    PAYMENT_PROCESS_DEPOSIT("Process deposit payment"),
    PAYMENT_PROCESS_WITHDRAWAL("Process withdrawal payment"),

    // Compensation Commands
    START_COMPENSATION("Start compensation"),
    ACCOUNT_MARK_TRANSACTION_FAILED("Mark transaction as failed"),
    PAYMENT_REVERSE_DEPOSIT("Reverse deposit payment"),
    PAYMENT_REVERSE_WITHDRAWAL("Reverse withdrawal payment"),
    ACCOUNT_DEPOSIT_REVERSE_BALANCE_UPDATE("Reverse balance update"),
    ACCOUNT_WITHDRAWAL_REVERSE_BALANCE_UPDATE("Reverse balance update"),
    ACCOUNT_REVERSE_SETTLEMENT("Reverse settlement - Compensation");
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
        } else if (this.name().startsWith("ORDER_")) {
            return "ORDER_SERVICE";
        } else if (this.name().startsWith("MARKET_")) {
            return "MARKET_DATA_SERVICE";
        } else if (this.name().startsWith("PORTFOLIO_")) {
            return "PORTFOLIO_SERVICE";
        } else if (this.name().startsWith("BROKER_")) {
            return "MOCK_BROKERAGE_SERVICE";
        } else {
            return "UNKNOWN_SERVICE";
        }
    }
}
