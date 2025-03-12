package com.project.userservice.payload.response.coinbase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinbaseApiRefreshResponse {
    private String access_token;
    private String refresh_token;
    private Long expires_in;
}