package com.stocktrading.kafka.model.enums;

import com.project.kafkamessagemodels.model.enums.CommandType;

/**
 * Enum defining all steps in the order sell saga
 */
public enum OrderSellSagaStep {
    // Normal flow steps
    CREATE_ORDER(1, "Create order", CommandType.ORDER_CREATE),
    VERIFY_TRADING_PERMISSION(2, "Verify trading permissions", CommandType.USER_VERIFY_TRADING_PERMISSIONS),
    VERIFY_ACCOUNT_STATUS(3, "Verify account status", CommandType.ACCOUNT_VERIFY_STATUS),
    VALIDATE_STOCK(4, "Validate stock exists", CommandType.MARKET_VALIDATE_STOCK),
    VERIFY_SUFFICIENT_SHARES(5, "Verify sufficient shares in portfolio", CommandType.PORTFOLIO_VERIFY_SHARES),
    GET_MARKET_PRICE(6, "Get current market price", CommandType.MARKET_GET_PRICE),
    RESERVE_SHARES(7, "Reserve shares in portfolio", CommandType.PORTFOLIO_RESERVE_SHARES),
    UPDATE_ORDER_VALIDATED(8, "Update order status to validated", CommandType.ORDER_UPDATE_VALIDATED),
    SUBMIT_ORDER(9, "Submit order for execution", CommandType.BROKER_EXECUTE_ORDER),
    UPDATE_ORDER_EXECUTED(10, "Update order status to executed", CommandType.ORDER_UPDATE_EXECUTED),
    CALCULATE_SETTLEMENT_AMOUNT(11, "Calculate settlement amount", null), // Performed in orchestrator
    SETTLE_TRANSACTION(12, "Settle transaction - credit funds", CommandType.ACCOUNT_SETTLE_TRANSACTION),
    UPDATE_PORTFOLIO(13, "Update portfolio positions - remove shares", CommandType.PORTFOLIO_UPDATE_POSITIONS),
    UPDATE_ORDER_COMPLETED(14, "Update order status to completed", CommandType.ORDER_UPDATE_COMPLETED),
    COMPLETE_SAGA(15, "Complete saga", null),

    // Compensation steps
    CANCEL_ORDER(101, "Cancel order", CommandType.ORDER_CANCEL),
    RELEASE_SHARES(102, "Release reserved shares", CommandType.PORTFOLIO_RELEASE_SHARES),
    CANCEL_BROKER_ORDER(103, "Cancel broker order", CommandType.BROKER_CANCEL_ORDER),
    REVERSE_FUNDS_CREDIT(104, "Reverse funds credit", CommandType.ACCOUNT_REVERSE_SETTLEMENT),
    RESTORE_PORTFOLIO(105, "Restore shares to portfolio", CommandType.PORTFOLIO_RESTORE_POSITIONS);

    private final int stepNumber;
    private final String description;
    private final CommandType commandType;

    OrderSellSagaStep(int stepNumber, String description, CommandType commandType) {
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
    public OrderSellSagaStep getNextStep() {
        switch (this) {
            case CREATE_ORDER:
                return VERIFY_TRADING_PERMISSION;
            case VERIFY_TRADING_PERMISSION:
                return VERIFY_ACCOUNT_STATUS;
            case VERIFY_ACCOUNT_STATUS:
                return VALIDATE_STOCK;
            case VALIDATE_STOCK:
                return VERIFY_SUFFICIENT_SHARES;
            case VERIFY_SUFFICIENT_SHARES:
                return GET_MARKET_PRICE;
            case GET_MARKET_PRICE:
                return RESERVE_SHARES;
            case RESERVE_SHARES:
                return UPDATE_ORDER_VALIDATED;
            case UPDATE_ORDER_VALIDATED:
                return SUBMIT_ORDER;
            case SUBMIT_ORDER:
                return UPDATE_ORDER_EXECUTED;
            case UPDATE_ORDER_EXECUTED:
                return CALCULATE_SETTLEMENT_AMOUNT;
            case CALCULATE_SETTLEMENT_AMOUNT:
                return SETTLE_TRANSACTION;
            case SETTLE_TRANSACTION:
                return UPDATE_PORTFOLIO;
            case UPDATE_PORTFOLIO:
                return UPDATE_ORDER_COMPLETED;
            case UPDATE_ORDER_COMPLETED:
                return COMPLETE_SAGA;
            default:
                return null;
        }
    }

    /**
     * Get next step in compensation flow
     */
    public OrderSellSagaStep getNextCompensationStep() {
        switch (this) {
            case RESTORE_PORTFOLIO:
                return REVERSE_FUNDS_CREDIT;
            case REVERSE_FUNDS_CREDIT:
                return CANCEL_BROKER_ORDER;
            case CANCEL_BROKER_ORDER:
                return RELEASE_SHARES;
            case RELEASE_SHARES:
                return CANCEL_ORDER;
            case CANCEL_ORDER:
                return COMPLETE_SAGA;
            default:
                return null;
        }
    }

    /**
     * Determine the first compensation step based on completed steps
     */
    public static OrderSellSagaStep determineFirstCompensationStep(
            boolean portfolioUpdated,
            boolean fundsSettled,
            boolean orderExecuted,
            boolean orderValidated,
            boolean sharesReserved) {

        if (portfolioUpdated) {
            return RESTORE_PORTFOLIO;
        } else if (fundsSettled) {
            return REVERSE_FUNDS_CREDIT;
        } else if (orderExecuted || orderValidated) {
            return CANCEL_BROKER_ORDER;
        } else if (sharesReserved) {
            return RELEASE_SHARES;
        } else {
            return CANCEL_ORDER;
        }
    }

    /**
     * Get step by step number
     */
    public static OrderSellSagaStep getByStepNumber(int stepNumber) {
        for (OrderSellSagaStep step : OrderSellSagaStep.values()) {
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