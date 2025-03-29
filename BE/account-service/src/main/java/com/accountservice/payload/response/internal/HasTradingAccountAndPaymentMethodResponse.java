package com.accountservice.payload.response.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HasTradingAccountAndPaymentMethodResponse {
    private boolean hasTradingAccount;
    private boolean hasPaymentMethods;
}
