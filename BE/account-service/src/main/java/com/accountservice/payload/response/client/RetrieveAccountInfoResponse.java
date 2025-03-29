package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrieveAccountInfoResponse {
    private String id;
    private String accountNumber;
    private String nickname;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private RetrieveBalanceInfoResponse balance;
}
