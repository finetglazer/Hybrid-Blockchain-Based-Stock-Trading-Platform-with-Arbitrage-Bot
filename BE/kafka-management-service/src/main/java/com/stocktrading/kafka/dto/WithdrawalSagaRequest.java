package com.stocktrading.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalSagaRequest {
    private String userId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethodId;
    private String description;
}
