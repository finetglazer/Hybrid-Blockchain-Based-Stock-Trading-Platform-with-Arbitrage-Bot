package com.stocktrading.orderservice.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.orderservice.model.Order;
import com.stocktrading.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    //value
    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${kafka.topics.order-events-sell}")
    private String orderEventsSellTopic;

    /**
     * Handle ORDER_CREATE command
     */
    public void handleCreateOrder(CommandMessage command) {
        log.info("Handling ORDER_CREATE command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        String orderType = command.getPayloadValue("orderType");
        Integer quantity = command.getPayloadValue("quantity");

        // Safe conversion for limitPrice - handle various number formats
        BigDecimal limitPrice = null;
        Object limitPriceObj = command.getPayloadValue("limitPrice");
        if (limitPriceObj != null) {
            if (limitPriceObj instanceof BigDecimal) {
                limitPrice = (BigDecimal) limitPriceObj;
            } else if (limitPriceObj instanceof Double) {
                limitPrice = BigDecimal.valueOf((Double) limitPriceObj);
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
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Create new order
            Order order = Order.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .accountId(accountId)
                    .stockSymbol(stockSymbol)
                    .orderType(orderType)
                    .side(Order.OrderSide.BUY) // Assuming this is a buy order
                    .quantity(quantity)
                    .limitPrice(limitPrice)
                    .timeInForce(timeInForce != null ? timeInForce : "DAY")
                    .status(Order.OrderStatus.CREATED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .sagaId(command.getSagaId())
                    .build();

            // Save the order
            Order savedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_CREATED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", savedOrder.getId());
            event.setPayloadValue("status", savedOrder.getStatus().name());
            event.setPayloadValue("createdAt", savedOrder.getCreatedAt().toString());
            event.setPayloadValue("order", savedOrder);

            log.info("Order created successfully with ID: {}", savedOrder.getId());

        } catch (Exception e) {
            log.error("Error creating order", e);
            handleOrderCreationFailure(event, "ORDER_CREATION_ERROR",
                    "Error creating order: " + e.getMessage());
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
            log.info("Sent ORDER_CREATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ORDER_UPDATE_VALIDATED command
     */
    public void handleUpdateOrderValidated(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_VALIDATED command for saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String reservationId = command.getPayloadValue("reservationId");
        Object priceObj = command.getPayloadValue("price");
        BigDecimal price = convertToBigDecimal(priceObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                handleOrderValidationFailure(event, "ORDER_NOT_FOUND",
                        "Order not found with ID: " + orderId, null);
                return;
            }

            Order order = orderOpt.get();

            // Verify order can be validated (initial state check)
            if (order.getStatus() != Order.OrderStatus.CREATED) {
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                handleOrderValidationFailure(event, "INVALID_ORDER_STATE",
                        "Order is not in CREATED state: " + order.getStatus(), order);
                return;
            }

            // Update order
            order.setStatus(Order.OrderStatus.VALIDATED);
            order.setReservationId(reservationId);
            if (price != null) {
                order.setExecutionPrice(price); // This might be either market price or limit price
            }
            order.setUpdatedAt(Instant.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_VALIDATED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("reservationId", updatedOrder.getReservationId());
            event.setPayloadValue("accountId", updatedOrder.getAccountId());
            event.setPayloadValue("order", updatedOrder);
            if (updatedOrder.getExecutionPrice() != null) {
                event.setPayloadValue("executionPrice", updatedOrder.getExecutionPrice());
            }

        } catch (Exception e) {
            log.error("Error validating order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            handleOrderValidationFailure(event, "ORDER_VALIDATION_ERROR",
                    "Error validating order: " + e.getMessage(), orderForFailure);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
            log.info("Sent ORDER_VALIDATED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle order creation failures
     */
    private void handleOrderCreationFailure(EventMessage event, String errorCode, String errorMessage) {
        event.setType("ORDER_CREATION_FAILED");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsTopic, event.getSagaId(), event);
            log.info("Sent ORDER_CREATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method to handle order validation failures
     */
    private void handleOrderValidationFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_VALIDATION_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsTopic, event.getSagaId(), event);
            log.info("Sent ORDER_VALIDATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Helper method to safely convert any numeric type to BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object amountObj) {
        if (amountObj instanceof BigDecimal) {
            return (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            return BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else if (amountObj instanceof String) {
            return new BigDecimal((String) amountObj);
        } else if (amountObj == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Amount is not a valid number: " + amountObj);
        }
    }

    /**
     * Handle ORDER_UPDATE_EXECUTED command
     */
    public void handleUpdateOrderExecuted(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_EXECUTED command for saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        BigDecimal executionPrice = convertToBigDecimal(command.getPayloadValue("executionPrice"));
        Integer executedQuantity = command.getPayloadValue("executedQuantity");
        String brokerOrderId = command.getPayloadValue("brokerOrderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                handleOrderExecutionUpdateFailure(event, "ORDER_NOT_FOUND",
                        "Order not found with ID: " + orderId, null);
                return;
            }

            Order order = orderOpt.get();

            // Verify order can be updated (state check)
            if (order.getStatus() != Order.OrderStatus.VALIDATED &&
                    order.getStatus() != Order.OrderStatus.EXECUTING) {
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                handleOrderExecutionUpdateFailure(event, "INVALID_ORDER_STATE",
                        "Order is not in a valid state for execution update: " + order.getStatus(), order);
                return;
            }

            // Update order
            order.setStatus(Order.OrderStatus.FILLED);
            order.setExecutionPrice(executionPrice);
            order.setExecutedQuantity(executedQuantity);
            order.setBrokerOrderId(brokerOrderId);
            order.setExecutedAt(Instant.now());
            order.setUpdatedAt(Instant.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_EXECUTED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("executionPrice", updatedOrder.getExecutionPrice());
            event.setPayloadValue("executedQuantity", updatedOrder.getExecutedQuantity());
            event.setPayloadValue("brokerOrderId", updatedOrder.getBrokerOrderId());
            event.setPayloadValue("executedAt", updatedOrder.getExecutedAt().toString());
            event.setPayloadValue("order", updatedOrder);

            log.info("Order execution updated successfully with ID: {}", updatedOrder.getId());

        } catch (Exception e) {
            log.error("Error updating order execution", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            handleOrderExecutionUpdateFailure(event, "ORDER_EXECUTION_UPDATE_ERROR",
                    "Error updating order execution: " + e.getMessage(), orderForFailure);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
            log.info("Sent ORDER_EXECUTED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle order execution update failures
     */
    private void handleOrderExecutionUpdateFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_EXECUTION_UPDATE_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsTopic, event.getSagaId(), event);
            log.info("Sent ORDER_EXECUTION_UPDATE_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ORDER_CANCEL command
     */
    public void handleCancelOrder(CommandMessage command) {
        log.info("Handling ORDER_CANCEL command for saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String reason = command.getPayloadValue("reason");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                // If order doesn't exist, still return success for saga continuation
                log.warn("Order not found with ID: {} during cancellation", orderId);

                event.setType("ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("status", "NOT_FOUND");
                event.setPayloadValue("note", "Order not found, assuming already cancelled");

                kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
                return;
            }

            Order order = orderOpt.get();

            // Check if order can be cancelled
            if (!order.canBeCancelled()) {
                log.warn("Order cannot be cancelled. Current status: {}", order.getStatus());

                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                // For compensation, we still want to continue the saga, so respond with success
                event.setType("ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("order", order);
                event.setPayloadValue("status", order.getStatus().name());
                event.setPayloadValue("note", "Order already in terminal state: " + order.getStatus());

                kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
                return;
            }

            // Update order status
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            order.setCancelledAt(Instant.now());

            // Add cancellation reason if provided
            if (reason != null && !reason.isEmpty()) {
                // Assuming there's a field for rejection reason that can be reused
                order.setRejectionReason(reason);
            }

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_CANCELLED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("cancelledAt", updatedOrder.getCancelledAt().toString());
            event.setPayloadValue("order", updatedOrder);

            log.info("Order cancelled successfully with ID: {}", updatedOrder.getId());

        } catch (Exception e) {
            log.error("Error cancelling order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            event.setType("ORDER_CANCELLATION_FAILED");
            event.setSuccess(false);
            event.setErrorCode("CANCELLATION_ERROR");
            event.setPayloadValue("order", orderForFailure);
            event.setErrorMessage("Error cancelling order: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
            log.info("Sent order cancellation response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ORDER_UPDATE_COMPLETED command
     */
    public void handleUpdateOrderCompleted(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_COMPLETED command for saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);

       try {
           // Find order
           Optional<Order> orderOpt = orderRepository.findById(orderId);
           if (orderOpt.isEmpty()) {
               handleOrderCompletionFailure(event, "ORDER_NOT_FOUND",
                       "Order not found with ID: " + orderId, null);
               return;
           }

           Order order = orderOpt.get();

           // Verify order can be completed (state check)
           if (order.getStatus() != Order.OrderStatus.FILLED) {
               order.setStatus(Order.OrderStatus.FAILED);
               orderRepository.save(order);
               handleOrderCompletionFailure(event, "INVALID_ORDER_STATE",
                       "Order is not in FILLED state: " + order.getStatus(), order);
               return;
           }

           // Update order to COMPLETED status
           order.setStatus(Order.OrderStatus.COMPLETED);
           order.setUpdatedAt(Instant.now());

           // Save updated order
           Order updatedOrder = orderRepository.save(order);

           // Set success response
           event.setType("ORDER_COMPLETED");
           event.setSuccess(true);
           event.setPayloadValue("orderId", updatedOrder.getId());
           event.setPayloadValue("status", updatedOrder.getStatus().name());
           event.setPayloadValue("completedAt", updatedOrder.getUpdatedAt().toString());
           event.setPayloadValue("order", updatedOrder);

           log.info("Order completed successfully with ID: {}", updatedOrder.getId());

       } catch (Exception e) {
           log.error("Error completing order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
           handleOrderCompletionFailure(event, "ORDER_COMPLETION_ERROR",
                   "Error completing order: " + e.getMessage(), orderForFailure);
           return;
       }

       // Send the response event
       try {
           kafkaTemplate.send(orderEventsTopic, command.getSagaId(), event);
           log.info("Sent ORDER_COMPLETED response for saga: {}", command.getSagaId());
       } catch (Exception e) {
           log.error("Error sending event", e);
       }
    }

    /**
     * Helper method to handle order completion failures
     */
    private void handleOrderCompletionFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_COMPLETION_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsTopic, event.getSagaId(), event);
            log.info("Sent ORDER_COMPLETION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ORDER_CREATE command for sell orders
     */
    public void handleCreateSellOrder(CommandMessage command) {
        log.info("Handling ORDER_CREATE command for sell saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");
        String accountId = command.getPayloadValue("accountId");
        String stockSymbol = command.getPayloadValue("stockSymbol");
        String orderType = command.getPayloadValue("orderType");
        Integer quantity = command.getPayloadValue("quantity");

        // Safe conversion for limitPrice - handle various number formats
        BigDecimal limitPrice = null;
        Object limitPriceObj = command.getPayloadValue("limitPrice");
        if (limitPriceObj != null) {
            if (limitPriceObj instanceof BigDecimal) {
                limitPrice = (BigDecimal) limitPriceObj;
            } else if (limitPriceObj instanceof Double) {
                limitPrice = BigDecimal.valueOf((Double) limitPriceObj);
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
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        try {
            // Create new sell order
            Order order = Order.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .accountId(accountId)
                    .stockSymbol(stockSymbol)
                    .orderType(orderType)
                    .side(Order.OrderSide.SELL) // This is a SELL order
                    .quantity(quantity)
                    .limitPrice(limitPrice)
                    .timeInForce(timeInForce != null ? timeInForce : "DAY")
                    .status(Order.OrderStatus.CREATED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .sagaId(command.getSagaId())
                    .build();

            // Save the order
            Order savedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_CREATED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", savedOrder.getId());
            event.setPayloadValue("status", savedOrder.getStatus().name());
            event.setPayloadValue("createdAt", savedOrder.getCreatedAt().toString());
            event.setPayloadValue("order", savedOrder);

            log.info("Sell order created successfully with ID: {}", savedOrder.getId());

        } catch (Exception e) {
            log.error("Error creating sell order", e);
            handleOrderCreationFailure(event, "ORDER_CREATION_ERROR",
                    "Error creating sell order: " + e.getMessage());
            return;
        }

        // Send the response event to the sell events topic
        try {
            // Use specific topic for sell orders
            kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
            log.info("Sent ORDER_CREATED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending sell event", e);
        }
    }

    /**
     * Handle ORDER_UPDATE_VALIDATED command for sell orders
     */
    public void handleUpdateSellOrderValidated(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_VALIDATED command for sell saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String reservationId = command.getPayloadValue("reservationId");
        Object priceObj = command.getPayloadValue("price");
        BigDecimal price = convertToBigDecimal(priceObj);

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                handleSellOrderValidationFailure(event, "ORDER_NOT_FOUND",
                        "Order not found with ID: " + orderId, null);
                return;
            }

            Order order = orderOpt.get();

            // Verify this is a sell order
            if (order.getSide() != Order.OrderSide.SELL) {
                handleSellOrderValidationFailure(event, "INVALID_ORDER_SIDE",
                        "Order is not a sell order: " + orderId, order);
                return;
            }

            // Verify order can be validated (initial state check)
            if (order.getStatus() != Order.OrderStatus.CREATED) {
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                handleSellOrderValidationFailure(event, "INVALID_ORDER_STATE",
                        "Order is not in CREATED state: " + order.getStatus(), order);
                return;
            }

            // Update order
            order.setStatus(Order.OrderStatus.VALIDATED);
            order.setReservationId(reservationId);
            if (price != null) {
                order.setExecutionPrice(price); // This might be either market price or limit price
            }
            order.setUpdatedAt(Instant.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_VALIDATED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("reservationId", updatedOrder.getReservationId());
            event.setPayloadValue("accountId", updatedOrder.getAccountId());
            event.setPayloadValue("order", updatedOrder);
            if (updatedOrder.getExecutionPrice() != null) {
                event.setPayloadValue("executionPrice", updatedOrder.getExecutionPrice());
            }

        } catch (Exception e) {
            log.error("Error validating sell order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            handleSellOrderValidationFailure(event, "ORDER_VALIDATION_ERROR",
                    "Error validating sell order: " + e.getMessage(), orderForFailure);
            return;
        }

        // Send the response event to sell orders topic
        try {
            kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
            log.info("Sent ORDER_VALIDATED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle sell order validation failures
     */
    private void handleSellOrderValidationFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_VALIDATION_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsSellTopic, event.getSagaId(), event);
            log.info("Sent ORDER_VALIDATION_FAILED response for sell saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
    /**
     * Handle ORDER_UPDATE_EXECUTED command for sell orders
     */
    public void handleUpdateSellOrderExecuted(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_EXECUTED command for sell saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        BigDecimal executionPrice = convertToBigDecimal(command.getPayloadValue("executionPrice"));
        Integer executedQuantity = command.getPayloadValue("executedQuantity");
        String brokerOrderId = command.getPayloadValue("brokerOrderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                handleSellOrderExecutionUpdateFailure(event, "ORDER_NOT_FOUND",
                        "Order not found with ID: " + orderId, null);
                return;
            }

            Order order = orderOpt.get();

            // Verify this is a sell order
            if (order.getSide() != Order.OrderSide.SELL) {
                handleSellOrderExecutionUpdateFailure(event, "INVALID_ORDER_SIDE",
                        "Order is not a sell order: " + orderId, order);
                return;
            }

            // Verify order can be updated (state check)
            if (order.getStatus() != Order.OrderStatus.VALIDATED &&
                    order.getStatus() != Order.OrderStatus.EXECUTING) {
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                handleSellOrderExecutionUpdateFailure(event, "INVALID_ORDER_STATE",
                        "Order is not in a valid state for execution update: " + order.getStatus(), order);
                return;
            }

            // Update order
            order.setStatus(Order.OrderStatus.FILLED);
            order.setExecutionPrice(executionPrice);
            order.setExecutedQuantity(executedQuantity);
            order.setBrokerOrderId(brokerOrderId);
            order.setExecutedAt(Instant.now());
            order.setUpdatedAt(Instant.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_EXECUTED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("executionPrice", updatedOrder.getExecutionPrice());
            event.setPayloadValue("executedQuantity", updatedOrder.getExecutedQuantity());
            event.setPayloadValue("brokerOrderId", updatedOrder.getBrokerOrderId());
            event.setPayloadValue("executedAt", updatedOrder.getExecutedAt().toString());
            event.setPayloadValue("order", updatedOrder);

            log.info("Sell order execution updated successfully with ID: {}", updatedOrder.getId());

        } catch (Exception e) {
            log.error("Error updating sell order execution", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            handleSellOrderExecutionUpdateFailure(event, "ORDER_EXECUTION_UPDATE_ERROR",
                    "Error updating sell order execution: " + e.getMessage(), orderForFailure);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
            log.info("Sent ORDER_EXECUTED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle sell order execution update failures
     */
    private void handleSellOrderExecutionUpdateFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_EXECUTION_UPDATE_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsSellTopic, event.getSagaId(), event);
            log.info("Sent ORDER_EXECUTION_UPDATE_FAILED response for sell saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }

    /**
     * Handle ORDER_CANCEL command for sell orders
     */
    public void handleCancelSellOrder(CommandMessage command) {
        log.info("Handling ORDER_CANCEL command for sell saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");
        String reason = command.getPayloadValue("reason");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);
        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                // If order doesn't exist, still return success for saga continuation
                log.warn("Sell order not found with ID: {} during cancellation", orderId);

                event.setType("ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("status", "NOT_FOUND");
                event.setPayloadValue("note", "Sell order not found, assuming already cancelled");

                kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
                return;
            }

            Order order = orderOpt.get();

            // Verify this is a sell order
            if (order.getSide() != Order.OrderSide.SELL) {
                log.warn("Order is not a sell order: {}", orderId);

                event.setType("ORDER_CANCELLATION_FAILED");
                event.setSuccess(false);
                event.setPayloadValue("order", order);
                event.setErrorCode("INVALID_ORDER_SIDE");
                event.setErrorMessage("Order is not a sell order: " + orderId);

                kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
                return;
            }

            // Check if order can be cancelled
            if (!order.canBeCancelled()) {
                log.warn("Sell order cannot be cancelled. Current status: {}", order.getStatus());

                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                // For compensation, we still want to continue the saga, so respond with success
                event.setType("ORDER_CANCELLED");
                event.setSuccess(true);
                event.setPayloadValue("orderId", orderId);
                event.setPayloadValue("order", order);
                event.setPayloadValue("status", order.getStatus().name());
                event.setPayloadValue("note", "Sell order already in terminal state: " + order.getStatus());

                kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
                return;
            }

            // Update order status
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            order.setCancelledAt(Instant.now());

            // Add cancellation reason if provided
            if (reason != null && !reason.isEmpty()) {
                // Assuming there's a field for rejection reason that can be reused
                order.setRejectionReason(reason);
            }

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_CANCELLED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("cancelledAt", updatedOrder.getCancelledAt().toString());
            event.setPayloadValue("order", updatedOrder);

            log.info("Sell order cancelled successfully with ID: {}", updatedOrder.getId());

        } catch (Exception e) {
            log.error("Error cancelling sell order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            event.setType("ORDER_CANCELLATION_FAILED");
            event.setSuccess(false);
            event.setErrorCode("CANCELLATION_ERROR");
            event.setPayloadValue("order", orderForFailure);
            event.setErrorMessage("Error cancelling sell order: " + e.getMessage());
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
            log.info("Sent order cancellation response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Handle ORDER_UPDATE_COMPLETED command for sell orders
     */
    public void handleUpdateSellOrderCompleted(CommandMessage command) {
        log.info("Handling ORDER_UPDATE_COMPLETED command for sell saga: {}", command.getSagaId());

        String orderId = command.getPayloadValue("orderId");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        Order orderForFailure = orderRepository.findById(orderId).orElse(null);

        try {
            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                handleSellOrderCompletionFailure(event, "ORDER_NOT_FOUND",
                        "Sell order not found with ID: " + orderId, null);
                return;
            }

            Order order = orderOpt.get();

            // Verify this is a sell order
            if (order.getSide() != Order.OrderSide.SELL) {
                handleSellOrderCompletionFailure(event, "INVALID_ORDER_SIDE",
                        "Order is not a sell order: " + orderId, order);
                return;
            }

            // Verify order can be completed (state check)
            if (order.getStatus() != Order.OrderStatus.FILLED) {
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                handleSellOrderCompletionFailure(event, "INVALID_ORDER_STATE",
                        "Sell order is not in FILLED state: " + order.getStatus(), order);
                return;
            }

            // Update order to COMPLETED status
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setUpdatedAt(Instant.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Set success response
            event.setType("ORDER_COMPLETED");
            event.setSuccess(true);
            event.setPayloadValue("orderId", updatedOrder.getId());
            event.setPayloadValue("status", updatedOrder.getStatus().name());
            event.setPayloadValue("completedAt", updatedOrder.getUpdatedAt().toString());
            event.setPayloadValue("order", updatedOrder);

            log.info("Sell order completed successfully with ID: {}", updatedOrder.getId());

        } catch (Exception e) {
            log.error("Error completing sell order", e);
            if (orderForFailure != null) {
                orderForFailure.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(orderForFailure);
            }
            handleSellOrderCompletionFailure(event, "ORDER_COMPLETION_ERROR",
                    "Error completing sell order: " + e.getMessage(), orderForFailure);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(orderEventsSellTopic, command.getSagaId(), event);
            log.info("Sent ORDER_COMPLETED response for sell saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event", e);
        }
    }

    /**
     * Helper method to handle sell order completion failures
     */
    private void handleSellOrderCompletionFailure(EventMessage event, String errorCode, String errorMessage, Order order) {
        event.setType("ORDER_COMPLETION_FAILED");
        event.setSuccess(false);
        event.setPayloadValue("order", order);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);

        try {
            kafkaTemplate.send(orderEventsSellTopic, event.getSagaId(), event);
            log.info("Sent ORDER_COMPLETION_FAILED response for sell saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
}
