package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentMethodRequest {
    private String verificationType;
    private PaymentMethodVerificationDataRequest verificationDataRequest;
}
