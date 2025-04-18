package com.stocktrading.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String accountId;

    @Indexed
    private String stockSymbol;

    private String orderType; // MARKET, LIMIT

    private OrderSide side; // BUY, SELL

    private Integer quantity;

    private BigDecimal limitPrice; // null for market orders

    private String timeInForce; // DAY, GTC, etc.

    private OrderStatus status;

    private BigDecimal executionPrice; // Filled when executed

    private Integer executedQuantity; // For partial fills

    private String brokerOrderId; // External reference

    private String reservationId; // Fund reservation ID in AccountService

    private String rejectionReason; // Filled if rejected

    private Instant createdAt;

    private Instant updatedAt;

    private Instant executedAt;

    private Instant cancelledAt;

    private String sagaId; // Reference to orchestration saga

    public enum OrderStatus {
        CREATED,
        VALIDATED,
        REJECTED,
        CANCELLED,
        EXPIRED,
        EXECUTING,
        PARTIALLY_FILLED,
        FILLED,
        COMPLETED,
        FAILED
    }

    public enum OrderSide {
        BUY,
        SELL
    }

    /**
     * Calculate the total value of the order
     */
    public BigDecimal getTotalValue() {
        if (executionPrice != null && executedQuantity != null && executedQuantity > 0) {
            return executionPrice.multiply(BigDecimal.valueOf(executedQuantity));
        } else if (limitPrice != null) {
            return limitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return null;
    }

    /**
     * Check if order is in a terminal state
     */
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REJECTED ||
                status == OrderStatus.FAILED ||
                status == OrderStatus.EXPIRED;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.CREATED ||
                status == OrderStatus.VALIDATED ||
                status == OrderStatus.EXECUTING ||
                status == OrderStatus.PARTIALLY_FILLED;
    }
}