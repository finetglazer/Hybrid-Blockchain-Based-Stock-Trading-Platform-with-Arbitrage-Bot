package com.accountservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
    @AllArgsConstructor
@NoArgsConstructor
public class UpdateTradingAccountResponse {
    private String accountId;
    private String accountNumber;
    private String nickname;
    private String status;
    private Instant updatedAt;
}
