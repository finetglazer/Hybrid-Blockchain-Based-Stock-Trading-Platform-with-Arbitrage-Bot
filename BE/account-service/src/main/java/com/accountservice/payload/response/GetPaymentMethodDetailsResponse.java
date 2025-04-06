package com.accountservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetPaymentMethodDetailsResponse {
    private String id;
    private String nickname;
    private String maskedNumber;
    private boolean isDefault;
    private String status;
    private Instant addedAt;
    private Instant updatedAt;
    private Instant lastUsedAt;
    private PaymentMethodMetadataResponse metadata;
    private PaymentMethodVerificationDetailsResponse verificationDetails;
}


// "id": "pm123456",
//         "type": "BANK_ACCOUNT",
//         "nickname": "My Primary Bank",
//         "maskedNumber": "XXXX6789",
//         "isDefault": true,
//         "status": "ACTIVE",
//         "addedAt": "2025-03-19T12:00:00Z",
//         "updatedAt": "2025-03-19T12:30:00Z",
//         "lastUsedAt": "2025-03-19T11:00:00Z",
//         "metadata": {
//         "accountType": "CHECKING",
//         "accountHolderName": "John Doe",
//         "bankName": "National Bank"
//         },
//         "verificationDetails": {
//         "verifiedAt": "2025-03-19T14:30:00Z",
//         "verificationMethod": "MICRO_DEPOSITS"
//         }
