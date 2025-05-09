package com.stocktrading.brokerage.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.brokerage.model.MockOrderBook;
import com.stocktrading.brokerage.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Service for handling order execution commands
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MockOrderBook mockOrderBook;
    private final Random random = new Random();

    @Value("${kafka.topics.broker-events}")
    private String brokerEventsTopic;

    @Value("${kafka.topics.broker-events.sell}")
    private String brokerSellEventsTopic;

    @Value("${market.simulation.order-execution-success-rate:90}")
    private int orderExecutionSuccessRate;

    /**
     * Handle BROKER_EXECUTE_ORDER command
     * Enhanced to support both BUY and SELL orders
     * @param command The Kafka command message
     * @param isSellOrder Flag indicating if this is a sell order (true) or buy order (false)
     */
    public void handleExecuteOrder(CommandMessage command, boolean isSellOrder) {
        log.info("Handling BROKER_EXECUTE_ORDER command for {} saga: {}",
                isSellOrder ? "SELL" : "BUY", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        String orderType = command.getPayloadValue("orderType");
        Integer quantity = command.getPayloadValue("quantity");
        String timeInForce = command.getPayloadValue("timeInForce");

        // Get the side (BUY or SELL) from the command or use default based on isSellOrder
        String side = command.getPayloadValue("side");
        if (side == null) {
            side = isSellOrder ? "SELL" : "BUY";
        }

        // Convert limit price to BigDecimal, handling different formats
        BigDecimal limitPrice = null;
        Object limitPriceObj = command.getPayloadValue("limitPrice");
        if (limitPriceObj != null) {
            if (limitPriceObj instanceof BigDecimal) {
                limitPrice = (BigDecimal) limitPriceObj;
            } else if (limitPriceObj instanceof Number) {
                limitPrice = BigDecimal.valueOf(((Number) limitPriceObj).doubleValue());
            } else if (limitPriceObj instanceof String) {
                limitPrice = new BigDecimal((String) limitPriceObj);
            }
        }

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("MOCK_BROKERAGE_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Simulate processing delay (100-500ms)
            simulateProcessingDelay();

            // Determine if order execution should succeed based on configured success rate
            boolean orderExecutionSucceeds = random.nextInt(100) < orderExecutionSuccessRate;

            if (!orderExecutionSucceeds) {
                handleOrderExecutionFailure(event, "EXECUTION_ERROR",
                        "Failed to execute order: market conditions not met", isSellOrder);
                return;
            }

            // Handle based on order type
            if ("LIMIT".equals(orderType)) {
                handleLimitOrder(command, event, orderId, stockSymbol, orderType, quantity,
                        limitPrice, timeInForce, side, isSellOrder);
            } else {
                // Standard MARKET order execution - immediate execution
                handleMarketOrder(event, orderId, stockSymbol, quantity, side, isSellOrder);
            }
        } catch (Exception e) {
            log.error("Error executing order", e);
            handleOrderExecutionFailure(event, "BROKER_SYSTEM_ERROR",
                    "System error while executing order: " + e.getMessage(), isSellOrder);
        }
    }

    /**
     * Handle LIMIT order execution
     */
    private void handleLimitOrder(CommandMessage command, EventMessage event, String orderId,
                                  String stockSymbol, String orderType, Integer quantity,
                                  BigDecimal limitPrice, String timeInForce, String side,
                                  boolean isSellOrder) {
        // Check if the limit price meets current market conditions for immediate execution
        boolean canExecuteImmediately = mockOrderBook.canExecuteImmediately(stockSymbol, side, limitPrice);

        if (canExecuteImmediately) {
            // Execute the order immediately
            BigDecimal executionPrice = mockOrderBook.getExecutionPrice(stockSymbol, side);

            String brokerOrderId = "MBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Set success response for immediate execution
            event.setType("ORDER_EXECUTED_BY_BROKER");
            event.setSuccess(true);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("brokerOrderId", brokerOrderId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("executionPrice", executionPrice);
            event.setPayloadValue("executedQuantity", quantity);
            event.setPayloadValue("executedAt", Instant.now().toString());
            event.setPayloadValue("status", "FILLED");
            event.setPayloadValue("side", side);

            log.info("Limit {} order executed immediately: {} for {} shares of {} at ${}",
                    side, brokerOrderId, quantity, stockSymbol, executionPrice);
        } else {
            // Add to order book for later execution
            PendingOrder pendingOrder = mockOrderBook.addPendingOrder(
                    orderId, stockSymbol, orderType, side, quantity,
                    limitPrice, timeInForce, command.getSagaId());

            // Return a "LIMIT_ORDER_QUEUED" event
            event.setType("LIMIT_ORDER_QUEUED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", orderId);
            event.setPayloadValue("stockSymbol", stockSymbol);
            event.setPayloadValue("limitPrice", limitPrice);
            event.setPayloadValue("currentPrice", mockOrderBook.getCurrentPrice(stockSymbol));
            event.setPayloadValue("queuedAt", Instant.now().toString());
            event.setPayloadValue("status", "QUEUED");
            event.setPayloadValue("side", side);

            if (pendingOrder.getExpirationTime() != null) {
                event.setPayloadValue("expiresAt", pendingOrder.getExpirationTime().toString());
            }

            log.info("Limit {} order queued in order book: {} for {} shares of {} at limit ${}",
                    side, orderId, quantity, stockSymbol, limitPrice);
        }

        // Publish the event
        publishEvent(event, isSellOrder);
    }

    /**
     * Handle MARKET order execution (immediate execution)
     */
    private void handleMarketOrder(EventMessage event, String orderId,
                                   String stockSymbol, Integer quantity,
                                   String side, boolean isSellOrder) {
        // Get current market price
        BigDecimal executionPrice = mockOrderBook.getCurrentPrice(stockSymbol);

        // Generate broker order ID
        String brokerOrderId = "MBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Set success response
        event.setType("ORDER_EXECUTED_BY_BROKER");
        event.setSuccess(true);
        event.setPayloadValue("orderId", orderId);
        event.setPayloadValue("brokerOrderId", brokerOrderId);
        event.setPayloadValue("stockSymbol", stockSymbol);
        event.setPayloadValue("executionPrice", executionPrice);
        event.setPayloadValue("executedQuantity", quantity);
        event.setPayloadValue("executedAt", Instant.now().toString());
        event.setPayloadValue("status", "FILLED");
        event.setPayloadValue("side", side);

        log.info("Market {} order executed: {} for {} shares of {} at ${}",
                side, brokerOrderId, quantity, stockSymbol, executionPrice);

        // Publish the event
        publishEvent(event, isSellOrder);
    }

    /**
     * Handle BROKER_CANCEL_ORDER command
     * Updated to handle both BUY and SELL orders
     */
    public void handleCancelOrder(CommandMessage command, boolean isSellOrder) {
        log.info("Handling BROKER_CANCEL_ORDER command for {} saga: {}",
                isSellOrder ? "SELL" : "BUY", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String brokerOrderId = command.getPayloadValue("brokerOrderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("MOCK_BROKERAGE_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // First, check if this is a pending limit order in our order book
            Optional<PendingOrder> pendingOrderOpt = mockOrderBook.findPendingOrder(orderId);

            if (pendingOrderOpt.isPresent()) {
                // This is a pending LIMIT order, remove it from the book
                mockOrderBook.removePendingOrder(orderId);

                event.setType("BROKER_ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("brokerOrderId", "LIMIT-PENDING-" + orderId);
                event.setPayloadValue("cancelledAt", Instant.now().toString());
                event.setPayloadValue("status", "CANCELLED");
                event.setPayloadValue("note", "Pending limit order cancelled");

                log.info("Pending limit order cancelled from order book: {}", orderId);
            }
            // Handle the case where brokerOrderId is null (order hasn't been sent to broker yet)
            else if (brokerOrderId == null) {
                log.info("No broker order ID provided for orderId: {}. No cancellation needed.", orderId);

                // Return success even though there was nothing to cancel
                event.setType("BROKER_ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("brokerOrderId", "NO_BROKER_ORDER");
                event.setPayloadValue("cancelledAt", Instant.now().toString());
                event.setPayloadValue("status", "NO_BROKER_ORDER");
                event.setPayloadValue("note", "Order hadn't been submitted to broker yet, no cancellation needed");
            } else {
                // Normal cancellation flow for existing broker orders
                // Simulate processing delay
                simulateProcessingDelay();

                event.setType("BROKER_ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("brokerOrderId", brokerOrderId);
                event.setPayloadValue("cancelledAt", Instant.now().toString());
                event.setPayloadValue("status", "CANCELLED");

                log.info("Order cancelled successfully: {}", brokerOrderId);
            }
        } catch (Exception e) {
            log.error("Error cancelling order", e);
            event.setType("BROKER_ORDER_CANCELLATION_FAILED");
            event.setSuccess(false);
            event.setErrorCode("CANCELLATION_ERROR");
            event.setErrorMessage("Error cancelling order: " + e.getMessage());
        }

        // Send the response event
        publishEvent(event, isSellOrder);
    }

    /**
     * Helper method to handle order execution failures
     */
    private void handleOrderExecutionFailure(EventMessage event, String errorCode,
                                             String errorMessage, boolean isSellOrder) {
        event.setType("ORDER_EXECUTION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        log.warn("Order execution failed: {} - {}", errorCode, errorMessage);

        // Publish the event
        publishEvent(event, isSellOrder);
    }

    /**
     * Publish event to Kafka
     * @param event The event to publish
     * @param isSellOrder Flag indicating if this is for a sell order (true) or buy order (false)
     */
    private void publishEvent(EventMessage event, boolean isSellOrder) {
        try {
            // Choose the appropriate topic based on order type
            String topicToUse = isSellOrder ? brokerSellEventsTopic : brokerEventsTopic;

            kafkaTemplate.send(topicToUse, event.getSagaId(), event);
            log.debug("Published event: {} for saga: {} to topic: {}",
                    event.getType(), event.getSagaId(), topicToUse);
        } catch (Exception e) {
            log.error("Error publishing event to Kafka", e);
        }
    }

    /**
     * Simulate processing delay
     */
    private void simulateProcessingDelay() {
        try {
            // Random delay between 100-500ms
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}