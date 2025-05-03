package com.stocktrading.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionInfo {
    private String stockSymbol;
    private int quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private Instant acquiredAt;
    private Instant updatedAt;
}
