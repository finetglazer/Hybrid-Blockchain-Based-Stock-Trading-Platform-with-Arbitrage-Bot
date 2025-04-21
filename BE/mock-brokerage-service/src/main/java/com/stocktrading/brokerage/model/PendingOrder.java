package com.stocktrading.brokerage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a pending limit order in the order book
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder {
    private String orderId;
    private String stockSymbol;
    private String orderType;  // LIMIT
    private String side;       // BUY or SELL
    private Integer quantity;
    private BigDecimal limitPrice;
    private String timeInForce; // DAY, GTC, etc.
    private String sagaId;     // Reference to the orchestrating saga
    private Instant createdAt;
    private Instant expirationTime; // When the order expires (null for GTC)

    /**
     * Check if the order has expired
     */
    public boolean isExpired() {
        if (expirationTime == null) {
            return false; // GTC orders don't expire
        }
        return Instant.now().isAfter(expirationTime);
    }
}