package com.stocktrading.kafka.model;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.stocktrading.kafka.model.enums.OrderSellSagaStep;
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
 * Represents the current state of an order sell saga
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order_sell_sagas")
public class OrderSellSagaState {
    // All existing fields remain unchanged
    @Id
    private String id;

    @Indexed(unique = true)
    private String sagaId;

    // Business data
    private String userId;
    private String accountId;
    private String stockSymbol;
    private String orderType; // Always set to "MARKET" in the simplified version
    private Integer quantity;
    private BigDecimal limitPrice; // Always null in the simplified version
    private String timeInForce; // Always set to "DAY" in the simplified version
    private String orderId;
    private BigDecimal executionPrice;
    private String reservationId;
    private Integer reservedQuantity;
    private Integer executedQuantity;
    private String brokerOrderId;
    private BigDecimal settlementAmount;

    // Saga execution state
    private OrderSellSagaStep currentStep;
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
     * Initialize a new order sell saga
     * This method remains unchanged but will always be called with hardcoded default values
     * for orderType, limitPrice, and timeInForce
     */
    public static OrderSellSagaState initiate(
            String sagaId, String userId, String accountId,
            String stockSymbol, String orderType, Integer quantity,
            BigDecimal limitPrice, String timeInForce, int maxRetries) {

        return OrderSellSagaState.builder()
                .id(sagaId)
                .sagaId(sagaId)
                .userId(userId)
                .accountId(accountId)
                .stockSymbol(stockSymbol)
                .orderType(orderType)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .timeInForce(timeInForce)
                .currentStep(OrderSellSagaStep.CREATE_ORDER)
                .status(SagaStatus.STARTED)
                .completedSteps(new ArrayList<>())
                .sagaEvents(new ArrayList<>())
                .stepData(new HashMap<>())
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(maxRetries)
                .reservedQuantity(0)
                .executedQuantity(0)
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
        OrderSellSagaStep nextStep = determineNextStep();
        currentStep = nextStep;
        currentStepStartTime = Instant.now();

        if (nextStep == OrderSellSagaStep.COMPLETE_SAGA) {
            status = SagaStatus.COMPLETED;
            endTime = Instant.now();
            addEvent("SAGA_COMPLETED", "Order sell saga completed successfully");
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
        boolean portfolioUpdated = completedSteps.contains(OrderSellSagaStep.UPDATE_PORTFOLIO.name());
        boolean fundsSettled = completedSteps.contains(OrderSellSagaStep.SETTLE_TRANSACTION.name());
        boolean orderExecuted = completedSteps.contains(OrderSellSagaStep.UPDATE_ORDER_EXECUTED.name());
        boolean orderValidated = completedSteps.contains(OrderSellSagaStep.UPDATE_ORDER_VALIDATED.name());
        boolean sharesReserved = completedSteps.contains(OrderSellSagaStep.RESERVE_SHARES.name());

        // Use the helper method to determine first compensation step
        currentStep = OrderSellSagaStep.determineFirstCompensationStep(
                portfolioUpdated,
                fundsSettled,
                orderExecuted,
                orderValidated,
                sharesReserved
        );

        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent("COMPENSATION_STEP", "Starting compensation with step: " + currentStep.getDescription());
    }

    /**
     * Start compensation with a specific first step
     */
    public void startCompensation(OrderSellSagaStep firstCompensationStep) {
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
        currentStep = OrderSellSagaStep.COMPLETE_SAGA; // Explicitly set current step to COMPLETE_SAGA
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

            case PORTFOLIO_VERIFY_SHARES:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", quantity);
                break;

            case MARKET_GET_PRICE:
                command.setPayloadValue("stockSymbol", stockSymbol);
                break;

            case PORTFOLIO_RESERVE_SHARES:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", quantity);
                command.setPayloadValue("orderId", orderId);
                break;

            case ORDER_CREATE:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("orderType", orderType);
                command.setPayloadValue("quantity", quantity);
                command.setPayloadValue("limitPrice", limitPrice);
                command.setPayloadValue("timeInForce", timeInForce);
                command.setPayloadValue("side", "SELL"); // Specify this is a SELL order
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
                command.setPayloadValue("side", "SELL"); // Specify this is a SELL order
                break;

            case ORDER_UPDATE_EXECUTED:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("executionPrice", executionPrice);
                command.setPayloadValue("executedQuantity", executedQuantity);
                command.setPayloadValue("brokerOrderId", brokerOrderId);
                break;

            case ACCOUNT_SETTLE_TRANSACTION:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("amount", settlementAmount);
                command.setPayloadValue("transactionType", "CREDIT"); // Credit funds for sell orders
                break;

            case PORTFOLIO_UPDATE_POSITIONS:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", -executedQuantity); // Negative for selling
                command.setPayloadValue("price", executionPrice);
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("operationType", "SELL");
                break;

            case ORDER_UPDATE_COMPLETED:
                command.setPayloadValue("orderId", orderId);
                break;

            // Compensation commands
            case ORDER_CANCEL:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("reason", failureReason);
                break;

            case PORTFOLIO_RELEASE_SHARES:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("reservationId", reservationId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("orderId", orderId);
                break;

            case BROKER_CANCEL_ORDER:
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("brokerOrderId", brokerOrderId);
                break;

            case ACCOUNT_REVERSE_SETTLEMENT:
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("amount", settlementAmount);
                command.setPayloadValue("transactionType", "DEBIT"); // Reverse the credit by debiting
                break;

            case PORTFOLIO_RESTORE_POSITIONS:
                command.setPayloadValue("userId", userId);
                command.setPayloadValue("accountId", accountId);
                command.setPayloadValue("stockSymbol", stockSymbol);
                command.setPayloadValue("quantity", executedQuantity); // Positive to restore the shares
                command.setPayloadValue("orderId", orderId);
                command.setPayloadValue("operationType", "RESTORE");
                break;
        }

        command.setMetadataValue("retryCount", String.valueOf(retryCount));
        return command;
    }

    /**
     * Determine the next step based on order type and current step
     */
    public OrderSellSagaStep determineNextStep() {
        // For LIMIT orders selling, we might want to customize the flow
        // For example, for LIMIT orders we might want to pause after submitting to broker

        // For now, follow the standard flow
        return currentStep.getNextStep();
    }
}