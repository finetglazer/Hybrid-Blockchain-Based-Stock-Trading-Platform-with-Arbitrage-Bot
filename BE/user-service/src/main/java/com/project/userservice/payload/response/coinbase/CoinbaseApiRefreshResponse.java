package com.project.userservice.payload.response.coinbase;

import lombok.Data;

@Data
public class CoinbaseApiRefreshResponse {
    private String access_token;
    private String refresh_token;
    private Long expires_in;
}