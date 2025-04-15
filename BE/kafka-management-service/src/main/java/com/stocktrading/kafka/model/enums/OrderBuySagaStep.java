package com.stocktrading.kafka.model.enums;

import com.project.kafkamessagemodels.model.enums.CommandType;

/**
 * Enum defining all steps in the order buy saga
 */
public enum OrderBuySagaStep {
    // Normal flow steps based on sequence diagram
    CREATE_ORDER(1, "Create order", CommandType.ORDER_CREATE),
    VERIFY_TRADING_PERMISSION(2, "Verify trading permissions", CommandType.USER_VERIFY_TRADING_PERMISSIONS),
    VERIFY_ACCOUNT_STATUS(3, "Verify account status", CommandType.ACCOUNT_VERIFY_STATUS),
    VALIDATE_STOCK(4, "Validate stock exists", CommandType.MARKET_VALIDATE_STOCK),
    GET_MARKET_PRICE(5, "Get current market price", CommandType.MARKET_GET_PRICE),
    CALCULATE_REQUIRED_FUNDS(6, "Calculate required funds", null), // Performed in orchestrator
    RESERVE_FUNDS(7, "Reserve funds", CommandType.ACCOUNT_RESERVE_FUNDS),
    UPDATE_ORDER_VALIDATED(8, "Update order status to validated", CommandType.ORDER_UPDATE_VALIDATED),
    SUBMIT_ORDER(9, "Submit order for execution", CommandType.BROKER_EXECUTE_ORDER),
    UPDATE_ORDER_EXECUTED(10, "Update order status to executed", CommandType.ORDER_UPDATE_EXECUTED),
    UPDATE_PORTFOLIO(11, "Update portfolio positions", CommandType.PORTFOLIO_UPDATE_POSITIONS),
    SETTLE_TRANSACTION(12, "Settle transaction", CommandType.ACCOUNT_SETTLE_TRANSACTION),
    UPDATE_ORDER_COMPLETED(13, "Update order status to completed", CommandType.ORDER_UPDATE_COMPLETED),
    COMPLETE_SAGA(14, "Complete saga", null),

    // Compensation steps (based on compensation document)
    COMP_CANCEL_ORDER(101, "Cancel order", CommandType.COMP_CANCEL_ORDER),
    COMP_RELEASE_FUNDS(102, "Release reserved funds", CommandType.COMP_RELEASE_FUNDS),
    COMP_CANCEL_BROKER_ORDER(103, "Cancel broker order", CommandType.COMP_CANCEL_BROKER_ORDER),
    COMP_REMOVE_POSITIONS(104, "Remove positions from portfolio", CommandType.COMP_REMOVE_POSITIONS),
    COMP_REVERSE_SETTLEMENT(105, "Reverse settlement", CommandType.COMP_REVERSE_SETTLEMENT);

    private final int stepNumber;
    private final String description;
    private final CommandType commandType;

    OrderBuySagaStep(int stepNumber, String description, CommandType commandType) {
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
    public OrderBuySagaStep getNextStep() {
        switch (this) {
            case CREATE_ORDER:
                return VERIFY_TRADING_PERMISSION;
            case VERIFY_TRADING_PERMISSION:
                return VERIFY_ACCOUNT_STATUS;
            case VERIFY_ACCOUNT_STATUS:
                return VALIDATE_STOCK;
            case VALIDATE_STOCK:
                return GET_MARKET_PRICE;
            case GET_MARKET_PRICE:
                return CALCULATE_REQUIRED_FUNDS;
            case CALCULATE_REQUIRED_FUNDS:
                return RESERVE_FUNDS;
            case RESERVE_FUNDS:
                return UPDATE_ORDER_VALIDATED;
            case UPDATE_ORDER_VALIDATED:
                return SUBMIT_ORDER;
            case SUBMIT_ORDER:
                return UPDATE_ORDER_EXECUTED;
            case UPDATE_ORDER_EXECUTED:
                return UPDATE_PORTFOLIO;
            case UPDATE_PORTFOLIO:
                return SETTLE_TRANSACTION;
            case SETTLE_TRANSACTION:
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
    public OrderBuySagaStep getNextCompensationStep() {
        switch (this) {
            case COMP_REVERSE_SETTLEMENT:
                return COMP_REMOVE_POSITIONS;
            case COMP_REMOVE_POSITIONS:
                return COMP_CANCEL_BROKER_ORDER;
            case COMP_CANCEL_BROKER_ORDER:
                return COMP_RELEASE_FUNDS;
            case COMP_RELEASE_FUNDS:
                return COMP_CANCEL_ORDER;
            case COMP_CANCEL_ORDER:
                return COMPLETE_SAGA;
            default:
                return null;
        }
    }

    /**
     * Determine the first compensation step based on completed steps
     */
    public static OrderBuySagaStep determineFirstCompensationStep(
            boolean transactionSettled,
            boolean portfolioUpdated,
            boolean orderExecuted,
            boolean orderValidated,
            boolean fundsReserved) {

        if (transactionSettled) {
            return COMP_REVERSE_SETTLEMENT;
        } else if (portfolioUpdated) {
            return COMP_REMOVE_POSITIONS;
        } else if (orderExecuted) {
            return COMP_CANCEL_BROKER_ORDER;
        } else if (orderValidated || fundsReserved) {
            return COMP_RELEASE_FUNDS;
        } else {
            return COMP_CANCEL_ORDER;
        }
    }

    /**
     * Get step by step number
     */
    public static OrderBuySagaStep getByStepNumber(int stepNumber) {
        for (OrderBuySagaStep step : OrderBuySagaStep.values()) {
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