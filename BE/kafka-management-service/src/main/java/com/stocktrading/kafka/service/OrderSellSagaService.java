package com.stocktrading.kafka.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.project.kafkamessagemodels.model.enums.EventType;
import com.stocktrading.kafka.exception.SagaNotFoundException;
import com.stocktrading.kafka.model.OrderSellSagaState;
import com.stocktrading.kafka.model.enums.OrderSellSagaStep;
import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.repository.OrderSellSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing the order sell saga workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSellSagaService {

    private final OrderSellSagaRepository orderSellSagaRepository;
    private final KafkaMessagePublisher messagePublisher;
    private final IdempotencyService idempotencyService;

    @Value("${saga.order.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${saga.timeout.default:30000}")
    private long defaultTimeout;

    /**
     * Start a new order sell saga - simplified version with default values
     */
    public OrderSellSagaState startSaga(String userId, String accountId,
                                        String stockSymbol, Integer quantity) {
        String sagaId = UUID.randomUUID().toString();

        // Use default values for the simplified version
        String orderType = "MARKET"; // Default to MARKET order
        BigDecimal limitPrice = null; // No limit price for MARKET orders
        String timeInForce = "DAY"; // Default time in force

        OrderSellSagaState saga = OrderSellSagaState.initiate(
                sagaId, userId, accountId, stockSymbol, orderType,
                quantity, limitPrice, timeInForce, maxRetries);

        orderSellSagaRepository.save(saga);

        // Process the first step
        processNextStep(saga);

        return saga;
    }

    /**
     * Process the next step in the saga
     */
    public void processNextStep(OrderSellSagaState saga) {
        log.debug("Processing step [{}] for saga: {}", saga.getCurrentStep(), saga.getSagaId());

        // Special handling for CALCULATE_SETTLEMENT_AMOUNT step
        if (saga.getCurrentStep() == OrderSellSagaStep.CALCULATE_SETTLEMENT_AMOUNT) {
            handleCalculateSettlementStep(saga);
            return;
        }

        CommandMessage command = saga.createCommandForCurrentStep();
        if (command == null) {
            // This can happen for the COMPLETE_SAGA step which doesn't have a command
            if (saga.getCurrentStep() == OrderSellSagaStep.COMPLETE_SAGA) {
                saga.moveToNextStep(); // This will mark the saga as COMPLETED
                orderSellSagaRepository.save(saga);
                log.info("Order sell saga [{}] completed successfully", saga.getSagaId());
            } else {
                log.warn("No command defined for step: {} in saga: {}",
                        saga.getCurrentStep(), saga.getSagaId());
            }
            return;
        }

        // Initialize the command
        command.initialize();

        // Save the updated saga state
        orderSellSagaRepository.save(saga);

        // Determine the topic based on the command type
        String targetTopic = getTopicForCommandType(CommandType.valueOf(command.getType()));

        // Publish the command
        messagePublisher.publishCommand(command, targetTopic);

        log.info("Published command [{}] for saga [{}] to topic: {}",
                command.getType(), saga.getSagaId(), targetTopic);
    }
    /**
     * Handle the CALCULATE_SETTLEMENT_AMOUNT step
     * This step is executed in the orchestrator without sending a command
     */
    private void handleCalculateSettlementStep(OrderSellSagaState saga) {
        try {
            log.debug("Calculating settlement amount for saga: {}", saga.getSagaId());

            // Get the execution price and quantity
            BigDecimal executionPrice = saga.getExecutionPrice();
            Integer executedQuantity = saga.getExecutedQuantity();

            if (executionPrice == null || executedQuantity == null) {
                throw new IllegalStateException("Execution price or quantity not available");
            }

            // Calculate settlement amount
            BigDecimal settlementAmount = executionPrice.multiply(new BigDecimal(executedQuantity));

            // Subtract any fees (example: 0.1% commission)
            BigDecimal commission = settlementAmount.multiply(new BigDecimal("0.001"));
            BigDecimal netSettlementAmount = settlementAmount.subtract(commission);

            // Store the amounts in the saga
            saga.setSettlementAmount(netSettlementAmount);
            saga.storeStepData("grossAmount", settlementAmount);
            saga.storeStepData("commission", commission);

            // Add event for tracking
            saga.addEvent("SETTLEMENT_CALCULATED",
                    "Settlement amount calculated: " + netSettlementAmount +
                            " (Gross: " + settlementAmount + ", Commission: " + commission + ")");

            // Move to the next step
            saga.moveToNextStep();
            orderSellSagaRepository.save(saga);

            // Process the next step (SETTLE_TRANSACTION)
            processNextStep(saga);
        } catch (Exception e) {
            log.error("Error calculating settlement amount", e);
            saga.handleFailure("Failed to calculate settlement amount: " + e.getMessage(),
                    OrderSellSagaStep.CALCULATE_SETTLEMENT_AMOUNT.name());
            orderSellSagaRepository.save(saga);

            // Start compensation
            startCompensation(saga);
        }
    }

    /**
     * Handle an event message response
     */
    public void handleEventMessage(EventMessage event) {
        String sagaId = event.getSagaId();
        log.debug("Handling event [{}] for saga: {}", event.getType(), sagaId);

        // Find the saga
        Optional<OrderSellSagaState> optionalSaga = orderSellSagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Received event for unknown saga: {}", sagaId);
            return;
        }

        OrderSellSagaState saga = optionalSaga.get();

        // Record processing to ensure idempotency
        if (idempotencyService.isProcessed(event)) {
            log.info("Event [{}] for saga [{}] has already been processed", event.getType(), sagaId);
            return;
        }

        // Special handling for LIMIT_ORDER_QUEUED event
        if ("LIMIT_ORDER_QUEUED".equals(event.getType())) {
            handleLimitOrderQueued(saga, event);
            return;
        }

        // Special handling for ORDER_EXPIRED event
        if ("ORDER_EXPIRED".equals(event.getType())) {
            handleOrderExpired(saga, event);
            return;
        }

        // Special handling for ORDER_EXECUTED_BY_BROKER event (for limit orders)
        if ("ORDER_EXECUTED_BY_BROKER".equals(event.getType()) &&
                saga.getStatus() == SagaStatus.LIMIT_ORDER_PENDING) {
            // Resume the saga from the paused state
            resumeLimitOrderSaga(saga, event);
            return;
        }

        // Check if this is a response to the current step
        boolean matchesCurrentStep = isEventForCurrentStep(saga, event);
        if (!matchesCurrentStep) {
            log.warn("Received event [{}] for saga [{}] but doesn't match current step [{}]",
                    event.getType(), sagaId, saga.getCurrentStep());

            // Record the event processing anyway
            Map<String, Object> result = new HashMap<>();
            result.put("ignored", true);
            result.put("reason", "Event does not match current step");
            idempotencyService.recordProcessing(event, result);

            return;
        }

        // Process the event based on success/failure
        if (Boolean.TRUE.equals(event.getSuccess())) {
            processSuccessEvent(saga, event);
        } else {
            processFailureEvent(saga, event);
        }

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("newStatus", saga.getStatus().name());
        result.put("newStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");
        idempotencyService.recordProcessing(event, result);
    }

    /**
     * Handle the LIMIT_ORDER_QUEUED event
     * This pauses the saga until the order is executed or expires
     */
    private void handleLimitOrderQueued(OrderSellSagaState saga, EventMessage event) {
        log.info("Limit order queued in broker order book: {}", saga.getOrderId());

        // Update saga state
        saga.setStatus(SagaStatus.LIMIT_ORDER_PENDING);
        saga.addEvent("LIMIT_ORDER_QUEUED", "Order added to broker order book for price monitoring");

        // Store expiration time if available
        String expiresAt = event.getPayloadValue("expiresAt");
        if (expiresAt != null) {
            saga.storeStepData("limitOrderExpiresAt", expiresAt);
            saga.addEvent("EXPIRATION_SET", "Order will expire at " + expiresAt);
        }

        // Store current market price for reference
        Object priceObj = event.getPayloadValue("currentPrice");
        if (priceObj != null) {
            BigDecimal currentPrice;
            if (priceObj instanceof BigDecimal) {
                currentPrice = (BigDecimal) priceObj;
            } else if (priceObj instanceof Double) {
                currentPrice = BigDecimal.valueOf((Double) priceObj);
            } else if (priceObj instanceof Number) {
                currentPrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                currentPrice = new BigDecimal((String) priceObj);
            } else {
                log.warn("Skipping currentMarketPrice storage due to unsupported format: {}",
                        priceObj.getClass().getName());
                currentPrice = null;
            }

            if (currentPrice != null) {
                saga.storeStepData("currentMarketPrice", currentPrice);
            }
        }

        saga.setLastUpdatedTime(Instant.now());
        orderSellSagaRepository.save(saga);

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("status", "LIMIT_ORDER_PENDING");
        result.put("action", "Saga paused waiting for price match or expiration");
        idempotencyService.recordProcessing(event, result);

        log.info("Saga [{}] paused waiting for limit order execution or expiration", saga.getSagaId());
    }

    /**
     * Handle the ORDER_EXPIRED event
     * This initiates compensation for an expired limit order
     */
    private void handleOrderExpired(OrderSellSagaState saga, EventMessage event) {
        log.info("Processing order expired event for saga: {}", saga.getSagaId());

        saga.setStatus(SagaStatus.FAILED);
        saga.setFailureReason("Order expired before execution conditions were met");
        saga.addEvent("ORDER_EXPIRED", "Limit order expired: " + event.getPayloadValue("expiredAt"));

        // Save the updated state
        saga.setLastUpdatedTime(Instant.now());
        orderSellSagaRepository.save(saga);

        // Start compensation
        startCompensation(saga);

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("status", "FAILED");
        result.put("action", "Started compensation for expired order");
        idempotencyService.recordProcessing(event, result);
    }

    /**
     * Resume a saga that was paused waiting for limit order execution
     */
    private void resumeLimitOrderSaga(OrderSellSagaState saga, EventMessage event) {
        log.info("Resuming saga [{}] after limit order execution", saga.getSagaId());

        // Update saga with execution details
        saga.setBrokerOrderId(event.getPayloadValue("brokerOrderId"));

        // Handle type conversion for executedQuantity
        Object quantityObj = event.getPayloadValue("executedQuantity");
        if (quantityObj instanceof Integer) {
            saga.setExecutedQuantity((Integer) quantityObj);
        } else if (quantityObj instanceof String) {
            saga.setExecutedQuantity(Integer.parseInt((String) quantityObj));
        } else if (quantityObj != null) {
            saga.setExecutedQuantity(((Number) quantityObj).intValue());
        }

        // Handle type conversion for executionPrice
        Object priceObj = event.getPayloadValue("executionPrice");
        if (priceObj instanceof BigDecimal) {
            saga.setExecutionPrice((BigDecimal) priceObj);
        } else if (priceObj instanceof Double) {
            saga.setExecutionPrice(BigDecimal.valueOf((Double) priceObj));
        } else if (priceObj instanceof Number) {
            saga.setExecutionPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
        } else if (priceObj instanceof String) {
            saga.setExecutionPrice(new BigDecimal((String) priceObj));
        }

        // Resume the saga at the UPDATE_ORDER_EXECUTED step
        saga.setStatus(SagaStatus.IN_PROGRESS);
        saga.setCurrentStep(OrderSellSagaStep.UPDATE_ORDER_EXECUTED);
        saga.setCurrentStepStartTime(Instant.now());
        saga.addEvent("LIMIT_ORDER_EXECUTED", "Limit order executed at price: " + saga.getExecutionPrice());

        // Save the updated state
        saga.setLastUpdatedTime(Instant.now());
        orderSellSagaRepository.save(saga);

        // Continue with next step
        processNextStep(saga);

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("status", "IN_PROGRESS");
        result.put("action", "Resumed saga at UPDATE_ORDER_EXECUTED step");
        idempotencyService.recordProcessing(event, result);
    }

    /**
     * Process a successful event
     */
    private void processSuccessEvent(OrderSellSagaState saga, EventMessage event) {
        log.info("Processing success event [{}] for saga [{}]", event.getType(), saga.getSagaId());

        // Update saga with event data based on event type
        updateSagaWithEventData(saga, event);

        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // For compensation steps, handle them differently
            handleCompensationStepSuccess(saga);
        } else {
            // Normal flow - move to the next step
            saga.moveToNextStep();
        }

        // Save the updated saga
        orderSellSagaRepository.save(saga);

        // Process the next step if saga is still active
        if (saga.getStatus() == SagaStatus.IN_PROGRESS ||
                saga.getStatus() == SagaStatus.COMPENSATING) {
            processNextStep(saga);
        }
    }

    /**
     * Handle successful completion of a compensation step
     */
    private void handleCompensationStepSuccess(OrderSellSagaState saga) {
        // Get the current compensation step
        OrderSellSagaStep currentStep = saga.getCurrentStep();

        log.debug("Handling successful completion of compensation step: {}", currentStep);

        // Add to completed steps
        if (saga.getCompletedSteps() == null) {
            saga.setCompletedSteps(new ArrayList<>());
        }
        saga.getCompletedSteps().add("COMP_" + currentStep.name());

        // Get the next compensation step
        OrderSellSagaStep nextStep = currentStep.getNextCompensationStep();
        log.debug("Next compensation step: {}", nextStep);

        if (nextStep == OrderSellSagaStep.COMPLETE_SAGA) {
            // All compensation steps completed
            saga.completeCompensation();
            log.info("Compensation process completed for saga [{}]", saga.getSagaId());
        } else {
            // More compensation steps to execute
            saga.setCurrentStep(nextStep);
            saga.setCurrentStepStartTime(Instant.now());
            saga.setLastUpdatedTime(Instant.now());
            saga.addEvent("COMPENSATION_STEP", "Moving to compensation step: " + nextStep.getDescription());
            log.info("Moving to next compensation step [{}] for saga [{}]",
                    nextStep.name(), saga.getSagaId());
        }
    }

    /**
     * Process a failure event
     */
    private void processFailureEvent(OrderSellSagaState saga, EventMessage event) {
        log.warn("Processing failure event [{}] for saga [{}]: {}",
                event.getType(), saga.getSagaId(), event.getErrorMessage());

        // Update saga with failure reason
        String failureReason = event.getErrorMessage();
        if (failureReason == null) {
            failureReason = "Failed in step: " + saga.getCurrentStep().name();
        }

        saga.handleFailure(failureReason, saga.getCurrentStep().name());

        // Start compensation based on the saga status
        if (saga.getStatus() == SagaStatus.LIMIT_ORDER_PENDING) {
            // For a limit order waiting in the order book
            saga.setBrokerOrderId(null); // Ensure this is null since order wasn't executed
            startCompensation(saga);
        } else if (saga.getStatus() == SagaStatus.FAILED) {
            if (isValidationStep(saga.getCurrentStep())) {
                // For validation steps, just terminate without compensation
                saga.setEndTime(Instant.now());
                saga.addEvent("SAGA_TERMINATED", "Saga terminated due to validation failure");
                orderSellSagaRepository.save(saga);
            } else {
                // For other steps, start compensation
                startCompensation(saga);
            }
        }
    }

    /**
     * Start the compensation process
     */
    private void startCompensation(OrderSellSagaState saga) {
        // Check which steps have been completed
        boolean portfolioUpdated = saga.getCompletedSteps().contains(OrderSellSagaStep.UPDATE_PORTFOLIO.name());
        boolean fundsSettled = saga.getCompletedSteps().contains(OrderSellSagaStep.SETTLE_TRANSACTION.name());
        boolean orderExecuted = saga.getCompletedSteps().contains(OrderSellSagaStep.UPDATE_ORDER_EXECUTED.name());
        boolean orderValidated = saga.getCompletedSteps().contains(OrderSellSagaStep.UPDATE_ORDER_VALIDATED.name());
        boolean sharesReserved = saga.getCompletedSteps().contains(OrderSellSagaStep.RESERVE_SHARES.name());

        // Start compensation
        saga.startCompensation(
                OrderSellSagaStep.determineFirstCompensationStep(
                        portfolioUpdated,
                        fundsSettled,
                        orderExecuted,
                        orderValidated,
                        sharesReserved
                )
        );

        orderSellSagaRepository.save(saga);
        processNextStep(saga);
    }

    /**
     * Check if the current step is a validation step (no compensation needed)
     */
    private boolean isValidationStep(OrderSellSagaStep step) {
        return step == OrderSellSagaStep.CREATE_ORDER ||
                step == OrderSellSagaStep.VERIFY_TRADING_PERMISSION ||
                step == OrderSellSagaStep.VERIFY_ACCOUNT_STATUS ||
                step == OrderSellSagaStep.VALIDATE_STOCK ||
                step == OrderSellSagaStep.VERIFY_SUFFICIENT_SHARES ||
                step == OrderSellSagaStep.GET_MARKET_PRICE;
    }

    /**
     * Update the saga state with data from the event
     */
    private void updateSagaWithEventData(OrderSellSagaState saga, EventMessage event) {
        try {
            // Store specific data based on event type
            switch (event.getType()) {
                case "ORDER_CREATED":
                    saga.setOrderId(event.getPayloadValue("orderId"));
                    break;

                case "SHARES_VALIDATED":
                    // Optionally store information about available shares
                    if (event.getPayload().containsKey("availableShares")) {
                        saga.storeStepData("availableShares", event.getPayloadValue("availableShares"));
                    }
                    break;

                case "PRICE_PROVIDED":
                    // Store market price for reference
                    saga.storeStepData("marketPrice", event.getPayloadValue("currentPrice"));
                    break;

                case "SHARES_RESERVED":
                    saga.setReservationId(event.getPayloadValue("reservationId"));
                    saga.setReservedQuantity(event.getPayloadValue("reservedQuantity"));
                    break;

                case "ORDER_EXECUTED_BY_BROKER":
                    saga.setBrokerOrderId(event.getPayloadValue("brokerOrderId"));
                    saga.setExecutedQuantity(event.getPayloadValue("executedQuantity"));
                    if (saga.getExecutionPrice() == null) {
                        // Handle type conversion for executionPrice
                        Object priceObj = event.getPayloadValue("executionPrice");
                        if (priceObj instanceof BigDecimal) {
                            saga.setExecutionPrice((BigDecimal) priceObj);
                        } else if (priceObj instanceof Double) {
                            saga.setExecutionPrice(BigDecimal.valueOf((Double) priceObj));
                        } else if (priceObj instanceof Number) {
                            saga.setExecutionPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                        } else if (priceObj instanceof String) {
                            saga.setExecutionPrice(new BigDecimal((String) priceObj));
                        }
                    }
                    break;

                // Handle other event types as needed
            }

            // Store any additional event payload data for future reference
            for (Map.Entry<String, Object> entry : event.getPayload().entrySet()) {
                saga.storeStepData(event.getType() + "_" + entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            log.error("Error updating saga with event data", e);
        }
    }

    /**
     * Check if this event is a response to the current saga step
     */
    private boolean isEventForCurrentStep(OrderSellSagaState saga, EventMessage event) {
        if (saga.getCurrentStep() == null) {
            return false;
        }

        // For compensation steps, we need special handling
        if (saga.getCurrentStep().isCompensationStep()) {
            return isEventForCompensationStep(saga, event);
        }

        try {
            EventType eventType = EventType.valueOf(event.getType());
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();

            // Get the command type associated with this event type
            CommandType eventCommandType = eventType.getAssociatedCommandType();

            // This is the key check - does this event correspond to the current step's command?
            boolean matchingCommandType = eventCommandType == expectedCommandType;

            // Check if the step ID matches (if provided)
            boolean matchingStepId = event.getStepId() == null ||
                    event.getStepId().equals(saga.getCurrentStep().getStepNumber());

            // Log details in case of mismatch for debugging
            if (!matchingCommandType) {
                log.debug("Event command type mismatch: event={}, expected={}",
                        eventCommandType, expectedCommandType);
            }

            if (!matchingStepId) {
                log.debug("Event step ID mismatch: event={}, current={}",
                        event.getStepId(), saga.getCurrentStep().getStepNumber());
            }

            return matchingCommandType && matchingStepId;

        } catch (Exception e) {
            log.error("Error checking if event matches current step: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if this event is a response to the current compensation step
     */
    private boolean isEventForCompensationStep(OrderSellSagaState saga, EventMessage event) {
        try {
            EventType eventType = EventType.valueOf(event.getType());
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();

            // Log detailed matching information for debugging
            log.debug("Compensation event matching: eventType={}, eventCommandType={}, expectedCommandType={}, stepId={}, currentStepNumber={}",
                    eventType, eventType.getAssociatedCommandType(), expectedCommandType,
                    event.getStepId(), saga.getCurrentStep().getStepNumber());

            // For compensation steps, use more flexible matching criteria
            // 1. The event type should match the expected command type
            boolean commandTypeMatches = eventType.getAssociatedCommandType() == expectedCommandType;

            // 2. If stepId is provided, it should match, but don't require it
            boolean stepIdMatches = event.getStepId() == null ||
                    event.getStepId().equals(saga.getCurrentStep().getStepNumber());

            // Return true if basic command type matching passes
            return commandTypeMatches && stepIdMatches;
        } catch (Exception e) {
            log.error("Error checking if event matches compensation step: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the appropriate Kafka topic for a command type
     */
    private String getTopicForCommandType(CommandType commandType) {
        String commandName = commandType.name();

        if (commandName.startsWith("USER_")) {
            return "user.commands.order-sell";
        } else if (commandName.startsWith("ACCOUNT_")) {
            return "account.commands.order-sell";
        } else if (commandName.startsWith("ORDER_")) {
            return "order.commands.order-sell";
        } else if (commandName.startsWith("MARKET_")) {
            return "market.commands.order-sell";
        } else if (commandName.startsWith("PORTFOLIO_")) {
            return "portfolio.commands.order-sell";
        } else if (commandName.startsWith("BROKER_")) {
            return "broker.commands.order-sell";
        }

        return "saga.dlq"; // Default fallback
    }

    /**
     * Check for timed-out steps and handle them
     */
    public void checkForTimeouts() {
        log.debug("Checking for timed-out saga steps");

        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS);

        // Find sagas that might have timed out
        Instant cutoffTime = Instant.now().minus(Duration.ofMillis(defaultTimeout));

        List<OrderSellSagaState> potentiallyTimedOutSagas =
                orderSellSagaRepository.findPotentiallyTimedOutSagas(activeStatuses, cutoffTime);

        for (OrderSellSagaState saga : potentiallyTimedOutSagas) {
            handlePotentialTimeout(saga);
        }
    }

    /**
     * Handle a potentially timed-out saga
     */
    private void handlePotentialTimeout(OrderSellSagaState saga) {
        if (saga.getCurrentStep() == null) {
            return;
        }

        Duration timeout = Duration.ofMillis(defaultTimeout);

        if (saga.isCurrentStepTimedOut(timeout)) {
            log.warn("Step [{}] has timed out for saga: {}", saga.getCurrentStep(), saga.getSagaId());

            // Check if we can retry
            if (saga.getRetryCount() < saga.getMaxRetries()) {
                // Increment retry count
                saga.incrementRetryCount();
                saga.addEvent("RETRY", "Retrying step " + saga.getCurrentStep() + " after timeout");

                // Save and retry the step
                orderSellSagaRepository.save(saga);
                processNextStep(saga);

            } else {
                // We've exceeded retries, start compensation
                saga.handleFailure("Step timed out after " + saga.getMaxRetries() + " retries",
                        saga.getCurrentStep().name());
                startCompensation(saga);
            }
        }
    }

    /**
     * Repository access methods
     */
    public Optional<OrderSellSagaState> findById(String sagaId) {
        return orderSellSagaRepository.findById(sagaId);
    }

    public List<OrderSellSagaState> findActiveSagas() {
        List<SagaStatus> activeStatuses = Arrays.asList(
                SagaStatus.STARTED, SagaStatus.IN_PROGRESS,
                SagaStatus.COMPENSATING, SagaStatus.LIMIT_ORDER_PENDING);
        return orderSellSagaRepository.findByStatusIn(activeStatuses);
    }

    public List<OrderSellSagaState> findByUserId(String userId) {
        return orderSellSagaRepository.findByUserId(userId);
    }

    public OrderSellSagaState findByOrderId(String orderId) {
        return orderSellSagaRepository.findByOrderId(orderId);
    }

    /**
     * Cancel an order by user request
     */
    @Transactional
    public OrderSellSagaState cancelOrderByUser(String sagaId) {
        log.info("Processing cancellation request for saga: {}", sagaId);

        // Find the saga and lock it to prevent concurrent modifications
        Optional<OrderSellSagaState> optionalSaga = orderSellSagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Saga not found for cancellation: {}", sagaId);
            throw new SagaNotFoundException(sagaId);
        }

        OrderSellSagaState saga = optionalSaga.get();

        // Check if the saga can be cancelled in its current state
        if (saga.getStatus() != SagaStatus.LIMIT_ORDER_PENDING &&
                saga.getStatus() != SagaStatus.IN_PROGRESS) {
            String errorMsg = String.format("Cannot cancel order in state %s: %s",
                    saga.getStatus(), sagaId);
            log.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // If the saga is already in COMPLETED, FAILED, or COMPENSATION_COMPLETED state
        if (saga.getStatus() == SagaStatus.COMPLETED ||
                saga.getStatus() == SagaStatus.FAILED ||
                saga.getStatus() == SagaStatus.COMPENSATION_COMPLETED) {
            String errorMsg = String.format("Order is already in final state %s: %s",
                    saga.getStatus(), sagaId);
            log.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // If already cancelling, return the current state
        if (saga.getStatus() == SagaStatus.COMPENSATING ||
                saga.getStatus() == SagaStatus.CANCELLED_BY_USER) {
            log.info("Order is already being cancelled: {}", sagaId);
            return saga;
        }

        // Mark as user-cancelled
        saga.setStatus(SagaStatus.CANCELLED_BY_USER);
        saga.setFailureReason("Cancelled by user request");
        saga.addEvent("USER_CANCELLED", "Order cancellation requested by user");
        saga.setLastUpdatedTime(Instant.now());

        // Save the updated saga state
        orderSellSagaRepository.save(saga);

        log.info("Order marked as cancelled by user: {}", sagaId);

        // Start compensation process
        startCompensation(saga);

        // Return updated saga
        return saga;
    }
}