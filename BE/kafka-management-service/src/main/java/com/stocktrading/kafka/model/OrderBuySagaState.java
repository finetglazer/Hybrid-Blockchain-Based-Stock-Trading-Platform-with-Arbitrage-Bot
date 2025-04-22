package com.stocktrading.kafka.model;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.stocktrading.kafka.model.enums.OrderBuySagaStep;
import com.stocktrading.kafka.model.enums.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the current state of an order buy saga
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order_buy_sagas")
public class OrderBuySagaState {
    @Id
    private String id;

    @Indexed(unique = true)
    private String sagaId;

    // Business data
    private String userId;
    private String accountId;
    private String stockSymbol;
    private String orderType; // MARKET, LIMIT, etc.
    private Integer quantity;
    private BigDecimal limitPrice; // null for market orders
    private String timeInForce; // DAY, GTC, etc.
    private String orderId; // ID in OrderService
    private BigDecimal executionPrice; // Filled when executed
    private String reservationId; // Fund reservation ID
    private BigDecimal reservedAmount; // Amount reserved for order
    private Integer executedQuantity; // For partial fills
    private String brokerOrderId; // ID from broker

    // Saga execution state
    private OrderBuySagaStep currentStep;
    private SagaStatus status;
    private List<String> completedSteps;
    private List<SagaEvent> sagaEvents;
    private String failureReason;
    private Map<String, Object> stepData;

    // Timing information
    private Instant startTime;
    private Instant endTime;
    private Instant lastUpdatedTime;
    private Instant currentStepStartTime;

    // Retry information
    private int retryCount;
    private int maxRetries;

    /**
     * Initialize a new order buy saga
     */
    public static OrderBuySagaState initiate(
            String sagaId, String userId, String accountId,
            String stockSymbol, String orderType, Integer quantity,
            BigDecimal limitPrice, String timeInForce, int maxRetries) {

        return OrderBuySagaState.builder()
                .id(sagaId)
                .sagaId(sagaId)
                .userId(userId)
                .accountId(accountId)
                .stockSymbol(stockSymbol)
                .orderType(orderType)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .timeInForce(timeInForce)
                .currentStep(OrderBuySagaStep.CREATE_ORDER)
                .status(SagaStatus.STARTED)
                .completedSteps(new ArrayList<>())
                .sagaEvents(new ArrayList<>())
                .stepData(new HashMap<>())
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(maxRetries)
                .build();
    }

    /**
     * Move to the next step in the saga
     */
    public void moveToNextStep() {
        // Add current step to completed steps
        if (currentStep != null && !currentStep.isCompensationStep()) {
            completedSteps.add(currentStep.name());
        }

        // Use our custom logic to determine the next step
        OrderBuySagaStep nextStep = determineNextStep();
        currentStep = nextStep;
        currentStepStartTime = Instant.now();

        if (nextStep == OrderBuySagaStep.COMPLETE_SAGA) {
            status = SagaStatus.COMPLETED;
            endTime = Instant.now();
            addEvent("SAGA_COMPLETED", "Order buy saga completed successfully");
        } else {
            status = SagaStatus.IN_PROGRESS;
            addEvent("STEP_CHANGED", "Moving to step: " + nextStep.getDescription());
        }

        lastUpdatedTime = Instant.now();
    }

    /**
     * Handle a step failure
     */
    public void handleFailure(String reason, String stepName) {
        failureReason = reason;
        status = SagaStatus.FAILED;
        addEvent("STEP_FAILED", "Step " + stepName + " failed: " + reason);
        lastUpdatedTime = Instant.now();
    }

    /**
     * Start compensation process using the default first step determination
     */
    public void startCompensation() {
        status = SagaStatus.COMPENSATING;
        addEvent("COMPENSATION_STARTED", "Starting compensation process");

        // Check which steps have been completed to determine correct compensation chain
        boolean transactionSettled = completedSteps.contains(OrderBuySagaStep.SETTLE_TRANSACTION.name());
        boolean portfolioUpdated = completedSteps.contains(OrderBuySagaStep.UPDATE_PORTFOLIO.name());
        boolean orderExecuted = completedSteps.contains(OrderBuySagaStep.UPDATE_ORDER_EXECUTED.name());
        boolean orderValidated = completedSteps.contains(OrderBuySagaStep.UPDATE_ORDER_VALIDATED.name());
        boolean fundsReserved = completedSteps.contains(OrderBuySagaStep.RESERVE_FUNDS.name());

        // Use the helper method to determine first compensation step
        currentStep = OrderBuySagaStep.determineFirstCompensationStep(
                transactionSettled,
                portfolioUpdated,
                orderExecuted,
                orderValidated,
                fundsReserved
        );

        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent("COMPENSATION_STEP", "Starting compensation with step: " + currentStep.getDescription());
    }

    /**
     * Start compensation with a specific first step
     */
    public void startCompensation(OrderBuySagaStep firstCompensationStep) {
        status = SagaStatus.COMPENSATING;
        addEvent("COMPENSATION_STARTED", "Starting compensation process");

        currentStep = firstCompensationStep;
        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent("COMPENSATION_STEP", "Starting compensation with step: " + currentStep.getDescription());
    }

    /**
     * Complete compensation process
     */
    public void completeCompensation() {
        status = SagaStatus.COMPENSATION_COMPLETED;
        currentStep = OrderBuySagaStep.COMPLETE_SAGA; // Explicitly set current step to COMPLETE_SAGA
        endTime = Instant.now();
        addEvent("COMPENSATION_COMPLETED", "Compensation process completed");
        lastUpdatedTime = Instant.now();
    }

    /**
     * Add an event to the saga history
     */
    public void addEvent(String type, String description) {
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
        sagaEvents.add(SagaEvent.of(type, description));
    }

    /**
     * Store data for the current step
     */
    public void storeStepData(String key, Object value) {
        if (stepData == null) {
            stepData = new HashMap<>();
        }
        stepData.put(key, value);
    }

    /**
     * Get stored step data
     */
    @SuppressWarnings("unchecked")
    public <T> T getStepData(String key) {
        if (stepData == null) {
            return null;
        }
        return (T) stepData.get(key);
    }

    /**
     * Check if the current step has timed out
     */
    public boolean isCurrentStepTimedOut(Duration timeout) {
        if (currentStepStartTime == null) {
            return false;
        }
        return Duration.between(currentStepStartTime, Instant.now()).compareTo(timeout) > 0;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        retryCount++;
        lastUpdatedTime = Instant.now();
    }

    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= maxRetries;
    }

    /**
     * Create command based on current step
     */
    /**
     * Create command based on current step
     */
    public CommandMessage createCommandForCurrentStep() {
        CommandType commandType = currentStep.getCommandType();
        if (commandType == null) {
            return null;
        }

        CommandMessage command = new CommandMessage();
        command.setMessageId(null); // Will be auto-generated
        command.setSagaId(sagaId);
        command.setStepId(currentStep.getStepNumber());
        command.setType(commandType.name());
        command.setSourceService("SAGA_ORCHESTRATOR");
        command.setTargetService(commandType.getTargetService());
        command.setIsCompensation(currentStep.isCompensationStep());
        command.setTimestamp(Instant.now());

        // Add payload based on command type
        switch (commandType) {
            case USER_VERIFY_TRADING_PERMISSIONS:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("orderType", orderType);
                break;

            case ACCOUNT_VERIFY_STATUS:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("userId", userId);
                break;

            case MARKET_VALIDATE_STOCK:
                command.setPayloadValue("stockSymbol", stockSymbol);
                break;

            case MARKET_GET_PRICE:
                command.setPayloadValue("stockSymbol", stockSymbol);
                break;

            case ACCOUNT_RESERVE_FUNDS:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("amount", reservedAmount);
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                break;

            case ORDER_CREATE:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("orderType", orderType);
                command.setPayloadValue("quantity", quantity);
                command.setPayloadValue("limitPrice", limitPrice);
                command.setPayloadValue("timeInForce", timeInForce);
                break;

            case ORDER_UPDATE_VALIDATED:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("reservationId", reservationId);
                command.setPayloadValue("price", executionPrice); // Market price or limit price
                break;

            case BROKER_EXECUTE_ORDER:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("orderType", orderType);
                command.setPayloadValue("quantity", quantity);
                command.setPayloadValue("limitPrice", limitPrice);
                command.setPayloadValue("timeInForce", timeInForce);
                break;

            case ORDER_UPDATE_EXECUTED:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("executionPrice", executionPrice);
                command.setPayloadValue("executedQuantity", executedQuantity);
                command.setPayloadValue("brokerOrderId", brokerOrderId);
                break;

            case PORTFOLIO_UPDATE_POSITIONS:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", executedQuantity);
                command.setPayloadValue("price", executionPrice);
                command.setPayloadValue("orderId", orderId);
                break;

            case ACCOUNT_SETTLE_TRANSACTION:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("reservationId", reservationId);
                command.setPayloadValue("finalAmount", executionPrice.multiply(new BigDecimal(executedQuantity)));
                command.setPayloadValue("orderId", orderId);
                break;

            case ORDER_UPDATE_COMPLETED:
                command.setPayloadValue("orderId", orderId);
                break;

            // Compensation commands - using service-specific command types
            case ORDER_CANCEL:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("reason", failureReason);
                break;

            case ACCOUNT_RELEASE_FUNDS:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("reservationId", reservationId);
                command.setPayloadValue("orderId", orderId);
                break;

            case BROKER_CANCEL_ORDER:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("brokerOrderId", brokerOrderId);
                break;

            case PORTFOLIO_REMOVE_POSITIONS:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", executedQuantity);
                command.setPayloadValue("orderId", orderId);
                break;

            case ACCOUNT_REVERSE_SETTLEMENT:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("reservationId", reservationId);
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("amount", executionPrice.multiply(new BigDecimal(executedQuantity)));
                break;
        }

        command.setMetadataValue("retryCount", String.valueOf(retryCount));
        return command;
    }

    /**
     * Determine target service for a command
     */
    private String getTargetServiceForCommand(CommandType commandType) {
        String commandName = commandType.name();

        if (commandName.startsWith("USER_")) {
            return "USER_SERVICE";
        } else if (commandName.startsWith("ACCOUNT_") || commandName.equals("COMP_RELEASE_FUNDS") ||
                commandName.equals("COMP_REVERSE_SETTLEMENT")) {
            return "ACCOUNT_SERVICE";
        } else if (commandName.startsWith("ORDER_") || commandName.equals("COMP_CANCEL_ORDER")) {
            return "ORDER_SERVICE";
        } else if (commandName.startsWith("MARKET_")) {
            return "MARKET_DATA_SERVICE";
        } else if (commandName.startsWith("PORTFOLIO_") || commandName.equals("COMP_REMOVE_POSITIONS")) {
            return "PORTFOLIO_SERVICE";
        } else if (commandName.startsWith("BROKER_") || commandName.equals("COMP_CANCEL_BROKER_ORDER")) {
            return "MOCK_BROKERAGE_SERVICE";
        }

        return "UNKNOWN_SERVICE";
    }

    /**
     * Determine the next step based on order type and current step
     */
    public OrderBuySagaStep determineNextStep() {
        // For LIMIT orders, skip GET_MARKET_PRICE step after stock validation
        if (currentStep == OrderBuySagaStep.VALIDATE_STOCK && "LIMIT".equals(orderType)) {
            return OrderBuySagaStep.CALCULATE_REQUIRED_FUNDS;
        }

        // Otherwise, follow the normal flow
        return currentStep.getNextStep();
    }
}