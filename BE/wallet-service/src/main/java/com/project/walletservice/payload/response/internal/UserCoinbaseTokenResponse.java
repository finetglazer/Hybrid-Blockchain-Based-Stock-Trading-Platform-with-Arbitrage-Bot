package com.project.walletservice.payload.response.internal;

import lombok.Data;

@Data
public class UserCoinbaseTokenResponse {
    private boolean linked;
    private String accessToken;
    private String errorMessage; // in case user-service returns an error
    // optionally: private Instant expiresAt; // if you want that info
}
