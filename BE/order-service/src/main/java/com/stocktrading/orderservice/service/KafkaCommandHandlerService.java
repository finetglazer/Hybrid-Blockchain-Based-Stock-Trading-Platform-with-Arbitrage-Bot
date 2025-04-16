package com.stocktrading.orderservice.service;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.orderservice.model.Order;
import com.stocktrading.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
        BigDecimal limitPrice = command.getPayloadValue("limitPrice");
        String timeInForce = command.getPayloadValue("timeInForce");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setSourceService("ORDER_SERVICE");
        event.setTimestamp(Instant.now());

        handleOrderCreationFailure(event, "ORDER_CREATION_ERROR",
                "Error creating order: ");
//
//        try {
//            // Create new order
//            Order order = Order.builder()
//                    .id(UUID.randomUUID().toString())
//                    .userId(userId)
//                    .accountId(accountId)
//                    .stockSymbol(stockSymbol)
//                    .orderType(orderType)
//                    .side(Order.OrderSide.BUY) // Assuming this is a buy order
//                    .quantity(quantity)
//                    .limitPrice(limitPrice)
//                    .timeInForce(timeInForce != null ? timeInForce : "DAY")
//                    .status(Order.OrderStatus.CREATED)
//                    .createdAt(Instant.now())
//                    .updatedAt(Instant.now())
//                    .sagaId(command.getSagaId())
//                    .build();
//
//            // Save the order
//            Order savedOrder = orderRepository.save(order);
//
//            // Set success response
//            event.setType("ORDER_CREATED");
//            event.setSuccess(true);
//            event.setPayloadValue("orderId", savedOrder.getId());
//            event.setPayloadValue("status", savedOrder.getStatus().name());
//            event.setPayloadValue("createdAt", savedOrder.getCreatedAt().toString());
//
//            log.info("Order created successfully with ID: {}", savedOrder.getId());
//
//        } catch (Exception e) {
//            log.error("Error creating order", e);
//            handleOrderCreationFailure(event, "ORDER_CREATION_ERROR",
//                    "Error creating order: " + e.getMessage());
//            return;
//        }
//
//        // Send the response event
//        try {
//            kafkaTemplate.send("order.events", command.getSagaId(), event);
//            log.info("Sent ORDER_CREATED response for saga: {}", command.getSagaId());
//        } catch (Exception e) {
//            log.error("Error sending event", e);
//        }
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
            kafkaTemplate.send("order.events", event.getSagaId(), event);
            log.info("Sent ORDER_CREATION_FAILED response for saga: {} - {}",
                    event.getSagaId(), errorMessage);
        } catch (Exception e) {
            log.error("Error sending failure event", e);
        }
    }
}