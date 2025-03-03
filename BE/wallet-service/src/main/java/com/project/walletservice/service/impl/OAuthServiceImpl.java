package com.project.walletservice.service.impl;

import com.project.walletservice.client.UserServiceClient;
import com.project.walletservice.payload.response.UserCoinbaseTokenResponse;
import com.project.walletservice.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserServiceClient userServiceClient;

    @Override
    public String getValidAccessToken(String userId) {
        // 1) Call user-service to get a valid token
        UserCoinbaseTokenResponse tokenResponse = userServiceClient.getValidCoinbaseToken(userId);

        // 2) Check the response
        if (tokenResponse.getAccessToken() == null || !tokenResponse.isLinked()) {
            // user-service says user not linked or something went wrong
            return null;
        }

        // 3) Return the access token
        return tokenResponse.getAccessToken();
    }
}
