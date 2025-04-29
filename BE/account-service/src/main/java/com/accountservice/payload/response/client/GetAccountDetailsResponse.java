package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAccountDetailsResponse {
    private String id;
    private String userId;
    private String accountNumber;
    private String nickname;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private RetrieveBalanceInfoResponse balance;
    private Instant lastTransactionAt;
}
