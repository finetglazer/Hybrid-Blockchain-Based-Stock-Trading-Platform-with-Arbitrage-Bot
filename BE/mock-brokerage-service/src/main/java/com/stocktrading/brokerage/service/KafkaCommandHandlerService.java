package com.stocktrading.brokerage.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;

import com.stocktrading.brokerage.model.MockOrderBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Value("${market.simulation.order-execution-success-rate:90}")
    private int orderExecutionSuccessRate;

    /**
     * Handle BROKER_EXECUTE_ORDER command
     */
    public void handleExecuteOrder(CommandMessage command) {
        log.info("Handling BROKER_EXECUTE_ORDER command for saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        String orderType = command.getPayloadValue("orderType");
        Integer quantity = command.getPayloadValue("quantity");
        Object limitPriceObj = command.getPayloadValue("limitPrice");
        BigDecimal limitPrice = null;
        if (limitPriceObj != null) {
            if (limitPriceObj instanceof BigDecimal) {
                limitPrice = (BigDecimal) limitPriceObj;
            } else if (limitPriceObj instanceof Number) {
                limitPrice = BigDecimal.valueOf(((Number) limitPriceObj).doubleValue());
            } else if (limitPriceObj instanceof String) {
                limitPrice = new BigDecimal((String) limitPriceObj);
            }
        }
        String timeInForce = command.getPayloadValue("timeInForce");

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

            if (orderExecutionSucceeds) {
                // Generate execution details for success case
                String brokerOrderId = "MBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // Determine execution price using the mock order book
                BigDecimal executionPrice;
                if ("MARKET".equals(orderType)) {
                    // For market orders, use the current market price
                    executionPrice = mockOrderBook.getCurrentPrice(stockSymbol);
                } else if ("LIMIT".equals(orderType) && limitPrice != null) {
                    // For limit orders, check if limit price meets market conditions
                    BigDecimal marketPrice = mockOrderBook.getCurrentPrice(stockSymbol);

                    // For buy orders, limit price must be >= market ask price
                    if (limitPrice.compareTo(mockOrderBook.getAskPrice(stockSymbol)) >= 0) {
                        // Use a price between market price and limit price
                        executionPrice = marketPrice;
                    } else {
                        // Cannot execute since limit price is too low
                        handleOrderExecutionFailure(event, "LIMIT_PRICE_TOO_LOW",
                                "Cannot execute buy order: limit price " + limitPrice +
                                        " is below market ask price " + mockOrderBook.getAskPrice(stockSymbol));
                        return;
                    }
                } else {
                    // Default fallback for other order types
                    executionPrice = mockOrderBook.getCurrentPrice(stockSymbol);
                }

                // Sometimes execute partial fills (10% chance if quantity > 10)
                Integer executedQuantity = quantity;
                if (quantity > 10 && random.nextInt(100) < 10) {
                    executedQuantity = quantity - random.nextInt(quantity / 2);
                }

                // Set success response
                event.setType("ORDER_EXECUTED_BY_BROKER");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("brokerOrderId", brokerOrderId);
                event.setPayloadValue("stockSymbol", stockSymbol);
                event.setPayloadValue("executionPrice", executionPrice);
                event.setPayloadValue("executedQuantity", executedQuantity);
                event.setPayloadValue("executedAt", Instant.now().toString());
                event.setPayloadValue("status", "FILLED");

                log.info("Order executed successfully: {} for {} shares of {} at ${}",
                        brokerOrderId, executedQuantity, stockSymbol, executionPrice);
            } else {
                // Handle order execution failure
                handleOrderExecutionFailure(event, "EXECUTION_ERROR",
                        "Failed to execute order: market conditions not met");
                return;
            }

        } catch (Exception e) {
            log.error("Error executing order", e);
            handleOrderExecutionFailure(event, "BROKER_SYSTEM_ERROR",
                    "System error while executing order: " + e.getMessage());
            return;
        }

        // Send the response event
        publishEvent(event);
    }

    /**
     * Handle BROKER_CANCEL_ORDER command
     * Updated to handle null broker order IDs gracefully
     */
    public void handleCancelOrder(CommandMessage command) {
        log.info("Handling BROKER_CANCEL_ORDER command for saga: {}", command.getSagaId());

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
            // Handle the case where brokerOrderId is null (order hasn't been sent to broker yet)
            if (brokerOrderId == null) {
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
        publishEvent(event);
    }

    /**
     * Helper method to handle order execution failures
     */
    private void handleOrderExecutionFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("ORDER_EXECUTION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        log.warn("Order execution failed: {} - {}", errorCode, errorMessage);
    }

    /**
     * Publish event to Kafka
     */
    private void publishEvent(EventMessage event) {
        try {
            kafkaTemplate.send(brokerEventsTopic, event.getSagaId(), event);
            log.debug("Published event: {} for saga: {}", event.getType(), event.getSagaId());
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