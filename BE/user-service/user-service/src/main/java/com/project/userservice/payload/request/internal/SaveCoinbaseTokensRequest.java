package com.project.userservice.payload.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaveCoinbaseTokensRequest {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
