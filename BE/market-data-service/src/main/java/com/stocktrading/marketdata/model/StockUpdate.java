package com.stocktrading.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdate {
    private String symbol;
    private String company;
    private BigDecimal price;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private long volume;
    private String timestamp;
    private BigDecimal change;
    private BigDecimal changePercent;
}
