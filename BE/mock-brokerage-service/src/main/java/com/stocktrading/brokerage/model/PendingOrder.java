package com.stocktrading.brokerage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a pending limit order in the order book
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder {
    private String orderId;
    private String stockSymbol;
    private String orderType; // LIMIT, etc.
    private String side; // BUY, SELL
    private Integer quantity;
    private BigDecimal limitPrice;
    private String timeInForce; // DAY, GTC, etc.
    private Instant createdAt;
    private Instant expirationTime;
    private String sagaId; // Reference to the saga orchestrating this order
    private String brokerOrderId; // Will be set when executed
}