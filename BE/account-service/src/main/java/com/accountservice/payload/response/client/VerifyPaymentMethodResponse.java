package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentMethodResponse {
    private String id;
    private String status;
    private Instant verifiedAt;
}
