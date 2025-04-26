package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrievePaymentMethodResponse {
    private String id;
    private String nickname;
    private String maskedNumber;
    private String type;
    private boolean isDefault;
    private String status;
    private Instant addedAt;
    private Instant lastUsedAt;
    private Map<String, Object> metadata;

    // {
    //     id: "67efe208e5ef12698df2070d",
    //     nickname: "Hung's Updated Bank Account",
    //     status: "INACTIVE",
    //     type: "BANK_ACCOUNT",
    //     maskedNumber: "*****4321",
    //     isDefault: true,
    //     addedAt: "2025-04-04T13:43:36.182Z",
    //     lastUsedAt: "2025-04-04T13:43:36.182Z",
    //     metadata: {
    //         accountHolderName: "HungSenahihi",
    //         accountNumber: "987654321",
    //         bankName: "MB",
    //         routingNumber: "123456789",
    //         verificationMethod: "MICRO_DEPOSITS",
    //         verificationRequired: true,
    //         verifiedAt: null,
    // },
}
