package com.stocktrading.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for starting an order sell saga
 * Simplified version with only essential fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSellSagaRequest {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Account ID cannot be blank")
    private String accountId;

    @NotBlank(message = "Stock symbol cannot be blank")
    private String stockSymbol;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    // The following fields have been removed as per simplified scope:
    // private String orderType; // MARKET, LIMIT, etc.
    // private BigDecimal limitPrice; // Required for LIMIT orders
    // private String timeInForce = "DAY"; // Optional, defaults to DAY
}