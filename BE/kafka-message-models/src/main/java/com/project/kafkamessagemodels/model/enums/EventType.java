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
    USER_TRADING_PERMISSIONS_VERIFIED("User trading permissions verified"),
    USER_TRADING_PERMISSIONS_INVALID("User trading permissions invalid"),

    // Account Service Events
    ACCOUNT_VALIDATED("Account validated"),
    ACCOUNT_VALIDATION_FAILED("Account validation failed"),
    ACCOUNT_STATUS_VERIFIED("Account status verified"),
    ACCOUNT_STATUS_INVALID("Account status invalid"),
    PAYMENT_METHOD_VALID("Payment method validated"),
    PAYMENT_METHOD_INVALID("Payment method invalid"),
    BALANCE_VALID("Balance valid"),
    BALANCE_VALIDATION_ERROR("Balance validation error"),
    DEPOSIT_TRANSACTION_CREATED("Transaction created"),
    DEPOSIT_TRANSACTION_CREATION_FAILED("Transaction creation failed"),
    WITHDRAWAL_TRANSACTION_CREATED("Transaction created"),
    WITHDRAWAL_TRANSACTION_CREATION_FAILED("Transaction creation failed"),TRANSACTION_STATUS_UPDATED("Transaction status updated"),
    TRANSACTION_UPDATE_FAILED("Transaction status update failed"),
    FUNDS_RESERVED("Funds reserved for order"),
    FUNDS_RESERVATION_FAILED("Fund reservation failed"),
    TRANSACTION_SETTLED("Transaction settled"),
    TRANSACTION_SETTLEMENT_FAILED("Transaction settlement failed"),
    FUNDS_RELEASED("Reserved funds released"),
    FUNDS_RELEASE_FAILED("Fund release failed"),

    // Order Service Events
    ORDER_CREATED("Order created"),
    ORDER_CREATION_FAILED("Order creation failed"),
    ORDER_VALIDATED("Order validated"),
    ORDER_VALIDATION_FAILED("Order validation failed"),
    ORDER_EXECUTED("Order executed"),
    ORDER_EXECUTION_UPDATE_FAILED("Order execution update failed"),
    ORDER_COMPLETED("Order completed"),
    ORDER_COMPLETION_FAILED("Order completion failed"),
    ORDER_CANCELLED("Order cancelled"),
    ORDER_CANCELLATION_FAILED("Order cancellation failed"),

    // Market Data Service Events
    STOCK_VALIDATED("Stock validation successful"),
    STOCK_VALIDATION_FAILED("Stock validation failed"),
    PRICE_PROVIDED("Market price provided"),
    PRICE_RETRIEVAL_FAILED("Price retrieval failed"),

    // Portfolio Service Events
    POSITIONS_UPDATED("Portfolio positions updated"),
    POSITIONS_UPDATE_FAILED("Portfolio positions update failed"),
    POSITIONS_REMOVED("Positions removed from portfolio"),
    POSITIONS_REMOVAL_FAILED("Positions removal failed"),
    SHARES_VALIDATED("Sufficient shares validated"),
    SHARES_VALIDATION_FAILED("Shares validation failed"),
    SHARES_RESERVED("Shares reserved for order"),
    SHARES_RESERVATION_FAILED("Shares reservation failed"),
    SHARES_RELEASED("Reserved shares released"),
    SHARES_RELEASE_FAILED("Shares release failed"),
    PORTFOLIO_POSITIONS_RESTORED("Portfolio positions restored"),
    PORTFOLIO_POSITIONS_RESTORE_FAILED("Portfolio positions restore failed"),


    // Mock Brokerage Service Events
    ORDER_EXECUTED_BY_BROKER("Order executed by broker"),
    ORDER_EXECUTION_FAILED("Order execution failed"),
    BROKER_ORDER_CANCELLED("Broker order cancelled"),
    BROKER_ORDER_CANCELLATION_FAILED("Broker order cancellation failed"),
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
    TRANSACTION_MARK_FAILED_ERROR("Error marking transaction as failed"),
    SETTLEMENT_REVERSED("Settlement reversed"),
    SETTLEMENT_REVERSAL_FAILED("Settlement reversal failed");

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
        return switch (this) {
            case USER_IDENTITY_VERIFIED, USER_VERIFICATION_FAILED -> CommandType.USER_VERIFY_IDENTITY;
            case USER_TRADING_PERMISSIONS_VERIFIED, USER_TRADING_PERMISSIONS_INVALID ->
                    CommandType.USER_VERIFY_TRADING_PERMISSIONS;
            case ACCOUNT_VALIDATED, ACCOUNT_VALIDATION_FAILED -> CommandType.ACCOUNT_VALIDATE;
            case ACCOUNT_STATUS_VERIFIED, ACCOUNT_STATUS_INVALID -> CommandType.ACCOUNT_VERIFY_STATUS;
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
            case FUNDS_RESERVED, FUNDS_RESERVATION_FAILED ->
                    CommandType.ACCOUNT_RESERVE_FUNDS;
            case TRANSACTION_SETTLED, TRANSACTION_SETTLEMENT_FAILED ->
                    CommandType.ACCOUNT_SETTLE_TRANSACTION;
            case FUNDS_RELEASED, FUNDS_RELEASE_FAILED ->
                    CommandType.ACCOUNT_RELEASE_FUNDS;
            // Order service events
            case ORDER_CREATED, ORDER_CREATION_FAILED ->
                    CommandType.ORDER_CREATE;
            case ORDER_VALIDATED, ORDER_VALIDATION_FAILED ->
                    CommandType.ORDER_UPDATE_VALIDATED;
            case ORDER_EXECUTED, ORDER_EXECUTION_UPDATE_FAILED ->
                    CommandType.ORDER_UPDATE_EXECUTED;
            case ORDER_COMPLETED, ORDER_COMPLETION_FAILED ->
                    CommandType.ORDER_UPDATE_COMPLETED;
            case ORDER_CANCELLED, ORDER_CANCELLATION_FAILED ->
                    CommandType.ORDER_CANCEL;
            // Market data service events
            case STOCK_VALIDATED, STOCK_VALIDATION_FAILED ->
                    CommandType.MARKET_VALIDATE_STOCK;
            case PRICE_PROVIDED, PRICE_RETRIEVAL_FAILED ->
                    CommandType.MARKET_GET_PRICE;
            // Portfolio service events
            case POSITIONS_UPDATED, POSITIONS_UPDATE_FAILED ->
                    CommandType.PORTFOLIO_UPDATE_POSITIONS;
            case POSITIONS_REMOVED, POSITIONS_REMOVAL_FAILED ->
                    CommandType.PORTFOLIO_REMOVE_POSITIONS;
            // Brokerage service events
            case ORDER_EXECUTED_BY_BROKER, ORDER_EXECUTION_FAILED ->
                    CommandType.BROKER_EXECUTE_ORDER;
            case BROKER_ORDER_CANCELLED, BROKER_ORDER_CANCELLATION_FAILED ->
                    CommandType.BROKER_CANCEL_ORDER;
            case SETTLEMENT_REVERSED, SETTLEMENT_REVERSAL_FAILED ->
                    CommandType.ACCOUNT_REVERSE_SETTLEMENT;
            case SHARES_VALIDATED, SHARES_VALIDATION_FAILED -> CommandType.PORTFOLIO_VERIFY_SHARES;
            case SHARES_RESERVED, SHARES_RESERVATION_FAILED -> CommandType.PORTFOLIO_RESERVE_SHARES;
            case SHARES_RELEASED, SHARES_RELEASE_FAILED -> CommandType.PORTFOLIO_RELEASE_SHARES;
            case PORTFOLIO_POSITIONS_RESTORED, PORTFOLIO_POSITIONS_RESTORE_FAILED -> CommandType.PORTFOLIO_RESTORE_POSITIONS;
            default -> null; // Return null for any unhandled event types
        };
    }
}

/*
Looking at the error, the switch expression doesn't cover all possible enum values. I need to add the missing cases and a default case to handle all enum constants.

<!-- replace lines 110 to 166 -->
```java
        return switch (this) {
            case USER_IDENTITY_VERIFIED, USER_VERIFICATION_FAILED -> CommandType.USER_VERIFY_IDENTITY;
            case USER_TRADING_PERMISSIONS_VERIFIED, USER_TRADING_PERMISSIONS_INVALID -> CommandType.USER_VERIFY_TRADING_PERMISSIONS;
            case ACCOUNT_VALIDATED, ACCOUNT_VALIDATION_FAILED -> CommandType.ACCOUNT_VALIDATE;
            case ACCOUNT_STATUS_VERIFIED, ACCOUNT_STATUS_INVALID ->
                    CommandType.ACCOUNT_VERIFY_STATUS;
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
            case FUNDS_RESERVED, FUNDS_RESERVATION_FAILED ->
                    CommandType.ACCOUNT_RESERVE_FUNDS;

            case TRANSACTION_SETTLED, TRANSACTION_SETTLEMENT_FAILED ->
                    CommandType.ACCOUNT_SETTLE_TRANSACTION;

            case FUNDS_RELEASED, FUNDS_RELEASE_FAILED ->
                CommandType.ACCOUNT_RELEASE_FUNDS;
            // Order service events
            case ORDER_CREATED, ORDER_CREATION_FAILED ->
                CommandType.ORDER_CREATE;

            case ORDER_VALIDATED, ORDER_VALIDATION_FAILED ->
                CommandType.ORDER_UPDATE_VALIDATED;

            case ORDER_EXECUTED, ORDER_EXECUTION_UPDATE_FAILED ->
                CommandType.ORDER_UPDATE_EXECUTED;

            case ORDER_COMPLETED, ORDER_COMPLETION_FAILED ->
                CommandType.ORDER_UPDATE_COMPLETED;

            case ORDER_CANCELLED, ORDER_CANCELLATION_FAILED ->
                CommandType.ORDER_CANCEL;

            // Market data service events
            case STOCK_VALIDATED, STOCK_VALIDATION_FAILED ->
                CommandType.MARKET_VALIDATE_STOCK;

            case PRICE_PROVIDED, PRICE_RETRIEVAL_FAILED ->
                CommandType.MARKET_GET_PRICE;

            // Portfolio service events
            case POSITIONS_UPDATED, POSITIONS_UPDATE_FAILED ->
                CommandType.PORTFOLIO_UPDATE_POSITIONS;

            case POSITIONS_REMOVED, POSITIONS_REMOVAL_FAILED ->
                CommandType.PORTFOLIO_REMOVE_POSITIONS;

            // Brokerage service events
            case ORDER_EXECUTED_BY_BROKER, ORDER_EXECUTION_FAILED ->
                CommandType.BROKER_EXECUTE_ORDER;

            case BROKER_ORDER_CANCELLED, BROKER_ORDER_CANCELLATION_FAILED ->
                CommandType.BROKER_CANCEL_ORDER;
            case SETTLEMENT_REVERSED, SETTLEMENT_REVERSAL_FAILED ->
                // Updated to use the service-specific naming without COMP_ prefix
                CommandType.ACCOUNT_REVERSE_SETTLEMENT;

        };
```
 */