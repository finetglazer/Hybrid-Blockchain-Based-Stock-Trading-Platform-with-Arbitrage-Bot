package com.project.userservice.payload.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveCoinbaseTokensRequest {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
