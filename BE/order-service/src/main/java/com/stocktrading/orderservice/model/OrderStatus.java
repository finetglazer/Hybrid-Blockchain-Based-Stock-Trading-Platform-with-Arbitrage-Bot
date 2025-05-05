package com.stocktrading.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus {
    private String id;

    private String userId;

    private String accountId;

    private String stockSymbol;

    private String orderType; // MARKET, LIMIT

    private Order.OrderSide side; // BUY, SELL

    private Integer quantity;

    private BigDecimal limitPrice; // null for market orders

    private String timeInForce; // DAY, GTC, etc.

    private Order.OrderStatus status;

    private BigDecimal executionPrice; // Filled when executed

    private Integer executedQuantity; // For partial fills

    private String brokerOrderId; // External reference

    private String reservationId; // Fund reservation ID in AccountService

    private String rejectionReason; // Filled if rejected

    private Instant createdAt;

    private Instant updatedAt;

    private Instant executedAt;

    private Instant cancelledAt;

    private String sagaId;

    private boolean completed = false;

    private Float totalValue = 0f;
}
