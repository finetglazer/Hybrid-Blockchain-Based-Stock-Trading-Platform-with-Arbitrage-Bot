package com.project.userservice.payload.response;

import lombok.Data;

import java.time.Instant;

@Data
public class UserCoinbaseTokenResponse {
    private boolean linked;
    private String accessToken;        // If linked + valid
    private Instant expiresAt;         // Optionally return new expiry
    private String errorMessage;       // If something went wrong
}
