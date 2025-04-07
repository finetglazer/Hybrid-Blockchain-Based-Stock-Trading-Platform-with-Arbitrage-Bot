package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentMethodRequest {
    private String type;
    private String nickname;
    private boolean setAsDefault;
    private PaymentMethodDetailsRequest details;
}
