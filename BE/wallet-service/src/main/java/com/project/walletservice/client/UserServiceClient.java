package com.project.walletservice.client;


import com.project.walletservice.payload.response.UserCoinbaseTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.baseUrl}") // e.g. "http://user-service:8080"
    private String userServiceBaseUrl;

    public UserCoinbaseTokenResponse getValidCoinbaseToken(String userId) {
        // e.g., calling GET /internal/users/{userId}/coinbase/valid-token
        String url = userServiceBaseUrl
                + "/internal/users/" + userId + "/coinbase/valid-token";

        ResponseEntity<UserCoinbaseTokenResponse> response =
                restTemplate.getForEntity(url, UserCoinbaseTokenResponse.class);
        return response.getBody();
    }
}
