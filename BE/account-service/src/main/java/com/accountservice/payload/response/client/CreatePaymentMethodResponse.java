package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentMethodResponse {
    private String id;
    private String type;
    private String nickname;
    private String maskedNumber;
    private boolean isDefault;
    private String status;
    private Instant addedAt;
    private boolean verificationRequired;
    private String verificationInstructions;
}
