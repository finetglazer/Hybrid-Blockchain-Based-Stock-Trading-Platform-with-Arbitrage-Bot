package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrieveBalanceInfoResponse {
    private String currency;
    private Float available;
    private Float reserved;
    private Float total;
}
