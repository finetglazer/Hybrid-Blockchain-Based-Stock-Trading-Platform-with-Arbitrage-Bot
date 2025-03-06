package com.project.walletservice.client;


import com.project.userservice.payload.request.internal.SaveCoinbaseTokensRequest;
import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.payload.response.internal.UserCoinbaseTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.baseUrl}")       // Vary, depending on local, docker, or product environment
    private String userServiceBaseUrl;

    public UserCoinbaseTokenResponse getValidCoinbaseToken(String userId) {
        // e.g., calling GET /internal/users/{userId}/coinbase/valid-token
        String url = userServiceBaseUrl
                + "/internal/users/" + userId + "/coinbase/valid-token";

        ResponseEntity<UserCoinbaseTokenResponse> response =
                restTemplate.getForEntity(url, UserCoinbaseTokenResponse.class);
        return response.getBody();
    }

    public void saveCoinbaseTokens(String userId, String accessToken, String refreshToken, Long expiresIn) {
        String url = userServiceBaseUrl + "/internal/users/coinbase/save-tokens";
        SaveCoinbaseTokensRequest request = new SaveCoinbaseTokensRequest(
            userId,
            accessToken,
            refreshToken,
            expiresIn
        );
        restTemplate.postForObject(url, request, BaseResponse.class);
    }
}
