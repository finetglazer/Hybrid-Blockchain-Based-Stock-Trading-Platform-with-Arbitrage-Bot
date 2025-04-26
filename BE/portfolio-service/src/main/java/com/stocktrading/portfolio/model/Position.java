package com.stocktrading.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    private String stockSymbol;
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private Instant acquiredAt;
    private Instant updatedAt;

    /**
     * Calculate the current value of the position
     */
    public BigDecimal getCurrentValue() {
        if (currentPrice != null) {
            return currentPrice.multiply(BigDecimal.valueOf(quantity));
        } else if (averagePrice != null) {
            return averagePrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate unrealized profit/loss
     */
    public BigDecimal getUnrealizedPL() {
        if (currentPrice != null && averagePrice != null) {
            return currentPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate percentage gain/loss
     */
    public BigDecimal getPercentageChange() {
        if (currentPrice != null && averagePrice != null && !averagePrice.equals(BigDecimal.ZERO)) {
            return currentPrice.subtract(averagePrice)
                    .divide(averagePrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
}