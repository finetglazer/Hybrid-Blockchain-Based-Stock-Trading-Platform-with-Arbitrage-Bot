package com.project.kafkamessagemodels.model.enums;

/**
 * Enum defining all event types used in the system
 */
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
    TRANSACTION_CREATED("Transaction created"),
    TRANSACTION_CREATION_FAILED("Transaction creation failed"),
    TRANSACTION_STATUS_UPDATED("Transaction status updated"),
    TRANSACTION_UPDATE_FAILED("Transaction status update failed"),
    BALANCE_UPDATED("Balance updated"),
    BALANCE_UPDATE_FAILED("Balance update failed"),
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

    // Mock Brokerage Service Events
    ORDER_EXECUTED_BY_BROKER("Order executed by broker"),
    ORDER_EXECUTION_FAILED("Order execution failed"),
    BROKER_ORDER_CANCELLED("Broker order cancelled"),
    BROKER_ORDER_CANCELLATION_FAILED("Broker order cancellation failed"),

    // Payment Processor Events
    PAYMENT_PROCESSED("Payment processed"),
    PAYMENT_FAILED("Payment processing failed"),

    // Compensation Event Responses
    PAYMENT_REVERSAL_COMPLETED("Payment reversal completed"),
    PAYMENT_REVERSAL_FAILED("Payment reversal failed"),
    BALANCE_REVERSAL_COMPLETED("Balance reversal completed"),
    BALANCE_REVERSAL_FAILED("Balance reversal failed"),
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
        switch (this) {
            // User service events
            case USER_IDENTITY_VERIFIED:
            case USER_VERIFICATION_FAILED:
                return CommandType.USER_VERIFY_IDENTITY;

            case USER_TRADING_PERMISSIONS_VERIFIED:
            case USER_TRADING_PERMISSIONS_INVALID:
                return CommandType.USER_VERIFY_TRADING_PERMISSIONS;

            // Account service events
            case ACCOUNT_VALIDATED:
            case ACCOUNT_VALIDATION_FAILED:
                return CommandType.ACCOUNT_VALIDATE;

            case ACCOUNT_STATUS_VERIFIED:
            case ACCOUNT_STATUS_INVALID:
                return CommandType.ACCOUNT_VERIFY_STATUS;

            case PAYMENT_METHOD_VALID:
            case PAYMENT_METHOD_INVALID:
                return CommandType.PAYMENT_METHOD_VALIDATE;

            case TRANSACTION_CREATED:
            case TRANSACTION_CREATION_FAILED:
                return CommandType.ACCOUNT_CREATE_PENDING_TRANSACTION;

            case FUNDS_RESERVED:
            case FUNDS_RESERVATION_FAILED:
                return CommandType.ACCOUNT_RESERVE_FUNDS;

            case TRANSACTION_SETTLED:
            case TRANSACTION_SETTLEMENT_FAILED:
                return CommandType.ACCOUNT_SETTLE_TRANSACTION;

            case FUNDS_RELEASED:
            case FUNDS_RELEASE_FAILED:
                return CommandType.ACCOUNT_RELEASE_FUNDS;

            case TRANSACTION_STATUS_UPDATED:
            case TRANSACTION_UPDATE_FAILED:
                return CommandType.ACCOUNT_UPDATE_TRANSACTION_STATUS;

            case BALANCE_UPDATED:
            case BALANCE_UPDATE_FAILED:
                return CommandType.ACCOUNT_UPDATE_BALANCE;

            // Order service events
            case ORDER_CREATED:
            case ORDER_CREATION_FAILED:
                return CommandType.ORDER_CREATE;

            case ORDER_VALIDATED:
            case ORDER_VALIDATION_FAILED:
                return CommandType.ORDER_UPDATE_VALIDATED;

            case ORDER_EXECUTED:
            case ORDER_EXECUTION_UPDATE_FAILED:
                return CommandType.ORDER_UPDATE_EXECUTED;

            case ORDER_COMPLETED:
            case ORDER_COMPLETION_FAILED:
                return CommandType.ORDER_UPDATE_COMPLETED;

            case ORDER_CANCELLED:
            case ORDER_CANCELLATION_FAILED:
                return CommandType.ORDER_CANCEL;

            // Market data service events
            case STOCK_VALIDATED:
            case STOCK_VALIDATION_FAILED:
                return CommandType.MARKET_VALIDATE_STOCK;

            case PRICE_PROVIDED:
            case PRICE_RETRIEVAL_FAILED:
                return CommandType.MARKET_GET_PRICE;

            // Portfolio service events
            case POSITIONS_UPDATED:
            case POSITIONS_UPDATE_FAILED:
                return CommandType.PORTFOLIO_UPDATE_POSITIONS;

            case POSITIONS_REMOVED:
            case POSITIONS_REMOVAL_FAILED:
                return CommandType.PORTFOLIO_REMOVE_POSITIONS;

            // Brokerage service events
            case ORDER_EXECUTED_BY_BROKER:
            case ORDER_EXECUTION_FAILED:
                return CommandType.BROKER_EXECUTE_ORDER;

            case BROKER_ORDER_CANCELLED:
            case BROKER_ORDER_CANCELLATION_FAILED:
                return CommandType.BROKER_CANCEL_ORDER;

            // Payment processor events
            case PAYMENT_PROCESSED:
            case PAYMENT_FAILED:
                return CommandType.PAYMENT_PROCESS_DEPOSIT;

            // Compensation events
            case PAYMENT_REVERSAL_COMPLETED:
            case PAYMENT_REVERSAL_FAILED:
                return CommandType.PAYMENT_REVERSE_DEPOSIT;

            case BALANCE_REVERSAL_COMPLETED:
            case BALANCE_REVERSAL_FAILED:
                return CommandType.ACCOUNT_REVERSE_BALANCE_UPDATE;

            case TRANSACTION_MARKED_FAILED:
            case TRANSACTION_MARK_FAILED_ERROR:
                return CommandType.ACCOUNT_MARK_TRANSACTION_FAILED;

            case SETTLEMENT_REVERSED:
            case SETTLEMENT_REVERSAL_FAILED:
                // Updated to use the service-specific naming without COMP_ prefix
                return CommandType.ACCOUNT_REVERSE_SETTLEMENT;

            default:
                return null;
        }
    }
}