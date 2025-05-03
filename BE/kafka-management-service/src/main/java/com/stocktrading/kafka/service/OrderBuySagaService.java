package com.stocktrading.kafka.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.project.kafkamessagemodels.model.enums.CommandType;
import com.project.kafkamessagemodels.model.enums.EventType;
import com.stocktrading.kafka.exception.SagaNotFoundException;
import com.stocktrading.kafka.model.OrderBuySagaState;
import com.stocktrading.kafka.model.enums.OrderBuySagaStep;
import com.stocktrading.kafka.model.enums.SagaStatus;
import com.stocktrading.kafka.repository.OrderBuySagaRepository;
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
 * Service for managing the order buy saga workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBuySagaService {

    private final OrderBuySagaRepository orderBuySagaRepository;
    private final KafkaMessagePublisher messagePublisher;
    private final IdempotencyService idempotencyService;

    @Value("${saga.deposit.retry.max-attempts}")
    private int maxRetries;

    @Value("${saga.timeout.default}")
    private long defaultTimeout;

    /**
     * Start a new order buy saga
     */
    public OrderBuySagaState startSaga(String userId, String accountId,
                                       String stockSymbol, String orderType,
                                       Integer quantity, BigDecimal limitPrice,
                                       String timeInForce) {
        String sagaId = UUID.randomUUID().toString();

        // Validate order type and required fields
        if ("LIMIT".equals(orderType) && limitPrice == null) {
            throw new IllegalArgumentException("Limit price is required for LIMIT orders");
        }

        if (timeInForce == null) {
            timeInForce = "DAY"; // Default time in force
        }

        OrderBuySagaState saga = OrderBuySagaState.initiate(
                sagaId, userId, accountId, stockSymbol, orderType,
                quantity, limitPrice, timeInForce, maxRetries);

        orderBuySagaRepository.save(saga);

        // Process the first step
        processNextStep(saga);

        return saga;
    }

    /**
     * Process the next step in the saga
     */
    public void processNextStep(OrderBuySagaState saga) {
        log.debug("Processing step [{}] for saga: {}", saga.getCurrentStep(), saga.getSagaId());

        // Special handling for CALCULATE_REQUIRED_FUNDS step
        if (saga.getCurrentStep() == OrderBuySagaStep.CALCULATE_REQUIRED_FUNDS) {
            handleCalculateFundsStep(saga);
            return;
        }

        CommandMessage command = saga.createCommandForCurrentStep();
        if (command == null) {
            // This can happen for the COMPLETE_SAGA step which doesn't have a command
            if (saga.getCurrentStep() == OrderBuySagaStep.COMPLETE_SAGA) {
                saga.moveToNextStep(); // This will mark the saga as COMPLETED
                orderBuySagaRepository.save(saga);
                log.info("Order buy saga [{}] completed successfully", saga.getSagaId());
            } else {
                log.warn("No command defined for step: {} in saga: {}",
                        saga.getCurrentStep(), saga.getSagaId());
            }
            return;
        }

        // Initialize the command
        command.initialize();

        // Save the updated saga state
        orderBuySagaRepository.save(saga);

        // Determine the topic based on the command type
        String targetTopic = getTopicForCommandType(CommandType.valueOf(command.getType()));

        // Publish the command
        messagePublisher.publishCommand(command, targetTopic);

        log.info("Published command [{}] for saga [{}] to topic: {}",
                command.getType(), saga.getSagaId(), targetTopic);
    }
    /**
     * Handle the CALCULATE_REQUIRED_FUNDS step
     * This step is executed in the orchestrator without sending a command
     */
    private void handleCalculateFundsStep(OrderBuySagaState saga) {
        try {
            log.debug("Calculating required funds for saga: {}", saga.getSagaId());

            BigDecimal priceToUse;

            // For LIMIT orders, always use the limit price for calculations
            if ("LIMIT".equals(saga.getOrderType()) && saga.getLimitPrice() != null) {
                priceToUse = saga.getLimitPrice();
                log.debug("Using limit price for calculation: {}", priceToUse);
            } else {
                // For MARKET orders, get the price from the previous step
                Object priceObj = saga.getStepData("PRICE_PROVIDED_currentPrice");
                if (priceObj == null) {
                    throw new IllegalStateException("Market price not available");
                }

                // Handle different price formats
                if (priceObj instanceof BigDecimal) {
                    priceToUse = (BigDecimal) priceObj;
                } else if (priceObj instanceof Double) {
                    priceToUse = BigDecimal.valueOf((Double) priceObj);
                } else if (priceObj instanceof Number) {
                    priceToUse = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                } else if (priceObj instanceof String) {
                    priceToUse = new BigDecimal((String) priceObj);
                } else {
                    throw new IllegalStateException("Market price is in an unsupported format: " +
                            priceObj.getClass().getName());
                }

                log.debug("Using market price for calculation: {}", priceToUse);
            }

            // Calculate the amount to reserve
            BigDecimal requiredAmount = priceToUse.multiply(new BigDecimal(saga.getQuantity()));

            // Add a buffer for market fluctuations (5%)
            BigDecimal reserveBuffer = requiredAmount.multiply(new BigDecimal("0.05"));
            BigDecimal totalReserveAmount = requiredAmount.add(reserveBuffer);

            // Store the amounts in the saga
            saga.setExecutionPrice(priceToUse);
            saga.setReservedAmount(totalReserveAmount);

            // Add event for tracking
            saga.addEvent("FUNDS_CALCULATED", "Required funds calculated: " + totalReserveAmount);

            // Move to the next step
            saga.moveToNextStep();
            orderBuySagaRepository.save(saga);

            // Process the next step (RESERVE_FUNDS)
            processNextStep(saga);
        } catch (Exception e) {
            log.error("Error calculating required funds", e);
            saga.handleFailure("Failed to calculate required funds: " + e.getMessage(),
                    OrderBuySagaStep.CALCULATE_REQUIRED_FUNDS.name());
            orderBuySagaRepository.save(saga);

            // Cancel the order on calculation failure
            cancelOrder(saga);
        }
    }

    // Add this method to handle order cancellation
    private void cancelOrder(OrderBuySagaState saga) {
        try {
            // Only attempt cancellation if we have an order ID
            if (saga.getOrderId() != null) {
                log.info("Cancelling order {} due to funds calculation failure", saga.getOrderId());

                // Create a command to cancel the order
                CommandMessage command = new CommandMessage();
                command.initialize();
                command.setSagaId(saga.getSagaId());
                command.setType(CommandType.ORDER_CANCEL.name());
                command.setSourceService("SAGA_ORCHESTRATOR");
                command.setTargetService("ORDER_SERVICE");
                command.setPayloadValue("orderId", saga.getOrderId());
                command.setPayloadValue("reason", saga.getFailureReason());

                // Publish the command
                String topic = getTopicForCommandType(CommandType.ORDER_CANCEL);
                messagePublisher.publishCommand(command, topic);
                saga.addEvent("ORDER_CANCELLED", "Order cancelled due to funds calculation failure");
            } else {
                log.info("No order to cancel - funds calculation failed before order creation");
            }
            saga.setStatus(SagaStatus.COMPENSATION_COMPLETED);
            saga.setEndTime(Instant.now());
            orderBuySagaRepository.save(saga);

        } catch (Exception e) {
            log.error("Error cancelling order after funds calculation failure", e);
        }
    }

    /**
     * Handle an event message response
     */
    public void handleEventMessage(EventMessage event) {
        String sagaId = event.getSagaId();
        log.debug("Handling event [{}] for saga: {}", event.getType(), sagaId);

        // Find the saga
        Optional<OrderBuySagaState> optionalSaga = orderBuySagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Received event for unknown saga: {}", sagaId);
            return;
        }

        OrderBuySagaState saga = optionalSaga.get();

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
    private void handleLimitOrderQueued(OrderBuySagaState saga, EventMessage event) {
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
        orderBuySagaRepository.save(saga);

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
    private void handleOrderExpired(OrderBuySagaState saga, EventMessage event) {
        log.info("Processing order expired event for saga: {}", saga.getSagaId());

        saga.setStatus(SagaStatus.FAILED);
        saga.setFailureReason("Order expired before execution conditions were met");
        saga.addEvent("ORDER_EXPIRED", "Limit order expired: " + event.getPayloadValue("expiredAt"));

        // Save the updated state
        saga.setLastUpdatedTime(Instant.now());
        orderBuySagaRepository.save(saga);

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
    private void resumeLimitOrderSaga(OrderBuySagaState saga, EventMessage event) {
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
        saga.setCurrentStep(OrderBuySagaStep.UPDATE_ORDER_EXECUTED);
        saga.setCurrentStepStartTime(Instant.now());
        saga.addEvent("LIMIT_ORDER_EXECUTED", "Limit order executed at price: " + saga.getExecutionPrice());

        // Save the updated state
        saga.setLastUpdatedTime(Instant.now());
        orderBuySagaRepository.save(saga);

        // Continue with next step
        processNextStep(saga);

        // Record the event as processed
        Map<String, Object> result = new HashMap<>();
        result.put("status", "IN_PROGRESS");
        result.put("action", "Resumed saga at UPDATE_ORDER_EXECUTED step");
        idempotencyService.recordProcessing(event, result);
    }

    /**
     * Add additional method implementations for supporting LIMIT orders
     */

    /**
     * Called to add a SagaStatus.LIMIT_ORDER_PENDING to the existing states
     */
    private void addLimitOrderPendingStatus(OrderBuySagaState saga) {
        saga.getStepData().put("limitOrderPendingStep", saga.getCurrentStep().name());
        saga.setStatus(SagaStatus.LIMIT_ORDER_PENDING);
        saga.setLastUpdatedTime(Instant.now());
        saga.addEvent("LIMIT_ORDER_PENDING",
                "Saga paused while waiting for price conditions to be met");
        orderBuySagaRepository.save(saga);
    }


    /**
     * Process a successful event
     */
    private void processSuccessEvent(OrderBuySagaState saga, EventMessage event) {
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
        orderBuySagaRepository.save(saga);

        // Process the next step if saga is still active
        if (saga.getStatus() == SagaStatus.IN_PROGRESS ||
                saga.getStatus() == SagaStatus.COMPENSATING) {
            processNextStep(saga);
        }
    }

    /**
     * Handle successful completion of a compensation step
     */
    private void handleCompensationStepSuccess(OrderBuySagaState saga) {
        // Get the current compensation step
        OrderBuySagaStep currentStep = saga.getCurrentStep();

        log.debug("Handling successful completion of compensation step: {}", currentStep);

        // Add to completed steps
        if (saga.getCompletedSteps() == null) {
            saga.setCompletedSteps(new ArrayList<>());
        }
        saga.getCompletedSteps().add("COMP_" + currentStep.name());

        // Get the next compensation step
        OrderBuySagaStep nextStep = currentStep.getNextCompensationStep();
        log.debug("Next compensation step: {}", nextStep);

        if (nextStep == OrderBuySagaStep.COMPLETE_SAGA) {
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
    private void processFailureEvent(OrderBuySagaState saga, EventMessage event) {
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
                orderBuySagaRepository.save(saga);
            } else {
                // For other steps, start compensation
                startCompensation(saga);
            }
        }
    }

    /**
     * Start the compensation process
     */
    private void startCompensation(OrderBuySagaState saga) {
        // Check which steps have been completed
        boolean transactionSettled = saga.getCompletedSteps().contains(OrderBuySagaStep.SETTLE_TRANSACTION.name());
        boolean portfolioUpdated = saga.getCompletedSteps().contains(OrderBuySagaStep.UPDATE_PORTFOLIO.name());
        boolean orderExecuted = saga.getCompletedSteps().contains(OrderBuySagaStep.UPDATE_ORDER_EXECUTED.name());
        boolean orderValidated = saga.getCompletedSteps().contains(OrderBuySagaStep.UPDATE_ORDER_VALIDATED.name());
        boolean fundsReserved = saga.getCompletedSteps().contains(OrderBuySagaStep.RESERVE_FUNDS.name());

        // Start compensation
        saga.startCompensation(
                OrderBuySagaStep.determineFirstCompensationStep(
                        transactionSettled,
                        portfolioUpdated,
                        orderExecuted,
                        orderValidated,
                        fundsReserved
                )
        );

        orderBuySagaRepository.save(saga);
        processNextStep(saga);
    }

    /**
     * Check if the current step is a validation step (no compensation needed)
     */
    private boolean isValidationStep(OrderBuySagaStep step) {
        return step == OrderBuySagaStep.CREATE_ORDER ||
                step == OrderBuySagaStep.VERIFY_TRADING_PERMISSION ||
                step == OrderBuySagaStep.VERIFY_ACCOUNT_STATUS ||
                step == OrderBuySagaStep.VALIDATE_STOCK ||
                step == OrderBuySagaStep.GET_MARKET_PRICE ||
                step == OrderBuySagaStep.CALCULATE_REQUIRED_FUNDS;
    }

    /**
     * Update the saga state with data from the event
     */
    private void updateSagaWithEventData(OrderBuySagaState saga, EventMessage event) {
        try {
            // Store specific data based on event type
            switch (event.getType()) {
                case "ORDER_CREATED":
                    saga.setOrderId(event.getPayloadValue("orderId"));
                    break;

                case "PRICE_PROVIDED":
                    // Store market price for calculation step
                    saga.storeStepData("PRICE_PROVIDED_currentPrice", event.getPayloadValue("currentPrice"));
                    break;

                case "FUNDS_RESERVED":
                    saga.setReservationId(event.getPayloadValue("reservationId"));
                    break;

                case "ORDER_EXECUTED_BY_BROKER":
                    saga.setBrokerOrderId(event.getPayloadValue("brokerOrderId"));
                    saga.setExecutedQuantity(event.getPayloadValue("executedQuantity"));
                    if (saga.getExecutionPrice() == null) {
                        saga.setExecutionPrice(event.getPayloadValue("executionPrice"));
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
    private boolean isEventForCurrentStep(OrderBuySagaState saga, EventMessage event) {
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
    private boolean isEventForCompensationStep(OrderBuySagaState saga, EventMessage event) {
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
            return "user.commands.order-buy";
        } else if (commandName.startsWith("ACCOUNT_")) {
            return "account.commands.order-buy";
        } else if (commandName.startsWith("ORDER_")) {
            return "order.commands.order-buy";
        } else if (commandName.startsWith("MARKET_")) {
            return "market.commands.order-buy";
        } else if (commandName.startsWith("PORTFOLIO_")) {
            return "portfolio.commands.order-buy";
        } else if (commandName.startsWith("BROKER_")) {
            return "broker.commands.order-buy";
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

        List<OrderBuySagaState> potentiallyTimedOutSagas =
                orderBuySagaRepository.findPotentiallyTimedOutSagas(activeStatuses, cutoffTime);

        for (OrderBuySagaState saga : potentiallyTimedOutSagas) {
            handlePotentialTimeout(saga);
        }
    }

    /**
     * Handle a potentially timed-out saga
     */
    private void handlePotentialTimeout(OrderBuySagaState saga) {
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
                orderBuySagaRepository.save(saga);
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
    public Optional<OrderBuySagaState> findById(String sagaId) {
        return orderBuySagaRepository.findById(sagaId);
    }

    // Add status LIMIT_ORDER_PENDING to active statuses method
    public List<OrderBuySagaState> findActiveSagas() {
        List<SagaStatus> activeStatuses = Arrays.asList(
                SagaStatus.STARTED, SagaStatus.IN_PROGRESS,
                SagaStatus.COMPENSATING, SagaStatus.LIMIT_ORDER_PENDING);
        return orderBuySagaRepository.findByStatusIn(activeStatuses);
    }

    public List<OrderBuySagaState> findByUserId(String userId) {
        return orderBuySagaRepository.findByUserId(userId);
    }

    public OrderBuySagaState findByOrderId(String orderId) {
        return orderBuySagaRepository.findByOrderId(orderId);
    }

    /**
     * Cancel an order by user request
     *
     * @param sagaId The ID of the saga to cancel
     * @return The updated saga state
     * @throws SagaNotFoundException if the saga is not found
     * @throws IllegalStateException if the saga cannot be cancelled in its current state
     */
    @Transactional
    public OrderBuySagaState cancelOrderByUser(String sagaId) {
        log.info("Processing cancellation request for saga: {}", sagaId);

        // Find the saga and lock it to prevent concurrent modifications
        Optional<OrderBuySagaState> optionalSaga = orderBuySagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Saga not found for cancellation: {}", sagaId);
            throw new SagaNotFoundException(sagaId);
        }

        OrderBuySagaState saga = optionalSaga.get();

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
        orderBuySagaRepository.save(saga);

        log.info("Order marked as cancelled by user: {}", sagaId);

        // Start compensation process
        startCompensation(saga);

        // Return updated saga
        return saga;
    }
}