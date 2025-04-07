package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDetailsRequest {
    private String accountNumber;
    private String routingNumber;
    private String accountHolderName;
    private String bankName;
}
