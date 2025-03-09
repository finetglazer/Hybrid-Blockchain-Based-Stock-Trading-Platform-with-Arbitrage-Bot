package com.project.userservice.payload.response.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCoinbaseTokenResponse {
    private boolean linked;
    private String accessToken;        // If linked + valid
    private Instant expiresAt;         // Optionally return new expiry
    private String errorMessage;       // If something went wrong
}
