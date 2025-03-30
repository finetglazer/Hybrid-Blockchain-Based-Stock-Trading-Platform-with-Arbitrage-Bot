package com.accountservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTradingAccountRequest {
    private String currency;
    private String nickname;
}
