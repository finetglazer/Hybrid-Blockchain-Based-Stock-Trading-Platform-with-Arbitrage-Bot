package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrieveBalanceHistoryResponse {
    private String date;
    private Float openingBalance;
    private Float closingBalance;
    private Float deposits;
    private Float withdrawals;
    private Float tradesNet;    // closingBalance - openingBalance - deposits + withdrawals
    private Float fees;
}
