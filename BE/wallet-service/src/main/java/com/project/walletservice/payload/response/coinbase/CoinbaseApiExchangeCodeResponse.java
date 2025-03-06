package com.project.walletservice.payload.response.coinbase;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoinbaseApiExchangeCodeResponse {
    private String access_token;
    private Long expires_in;
    private String refresh_token;
}
