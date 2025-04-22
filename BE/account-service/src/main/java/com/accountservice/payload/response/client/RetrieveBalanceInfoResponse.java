package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrieveBalanceInfoResponse {
    private String currency;
    private BigDecimal available;
    private BigDecimal reserved;
    private BigDecimal total;
}
