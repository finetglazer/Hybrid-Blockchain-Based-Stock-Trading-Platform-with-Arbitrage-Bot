package com.project.walletservice.payload.response.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCoinbaseTokenResponse {
    private boolean linked;
    private String accessToken;
    private String errorMessage; // in case user-service returns an error
    // optionally: private Instant expiresAt; // if you want that info
}
