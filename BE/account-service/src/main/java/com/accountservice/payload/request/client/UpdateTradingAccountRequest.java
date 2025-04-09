package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTradingAccountRequest {
    private String accountId;
    private String nickname;
    private String status;
}
