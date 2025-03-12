package com.project.walletservice.service.impl;

import com.project.walletservice.client.UserServiceClient;
import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.common.Const;
import com.project.walletservice.payload.request.coinbase.CoinbaseApiExchangeCodeRequest;
import com.project.walletservice.payload.response.coinbase.CoinbaseApiExchangeCodeResponse;
import com.project.walletservice.payload.response.internal.UserCoinbaseTokenResponse;
import com.project.walletservice.service.OAuthService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {
    Dotenv dotenv = Dotenv.configure().directory("E:\\HTPT\\Hybrid Blockchain-Based Stock Trading Platform with Arbitrage Bot\\BE\\wallet-service\\.env").load();

    private final UserServiceClient userServiceClient;
    private final RestTemplate restTemplate;

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

    @Override
    public BaseResponse<?> exchangeAuthCode(String userId, String authCode) {
        // 1) Create exchange request to Coinbase to get tokens

        // Create request body
        CoinbaseApiExchangeCodeRequest requestBody = new CoinbaseApiExchangeCodeRequest(
                "authorization_code",
                authCode,
                dotenv.get("COINBASE_OAUTH_CLIENT_ID"),
                dotenv.get("COINBASE_OAUTH_CLIENT_SECRET"),
                dotenv.get("COINBASE_OAUTH_REDIRECT_URI")
        );

        // Create request header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CoinbaseApiExchangeCodeRequest> entity = new HttpEntity<>(requestBody, headers);

        // Call the Coinbase exchange code endpoint
        String coinbaseUrl = "https://login.coinbase.com/oauth2/token";
        // Adjust if your endpoint differs per actual Coinbase docs
        ResponseEntity<CoinbaseApiExchangeCodeResponse> response;

        try {
            response = restTemplate.postForEntity(
                    coinbaseUrl,
                    entity,
                    CoinbaseApiExchangeCodeResponse.class
            );
        } catch (RestClientException ex) {
            // handle error
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Coinbase API request failed:" + ex.getMessage(),
                    ""
            );
        }

        // Interpret the response
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // If success, save tokens
            CoinbaseApiExchangeCodeResponse responseBody = response.getBody();
            String accessToken = responseBody.getAccess_token();
            String refreshToken = responseBody.getRefresh_token();
            Long expiresIn = responseBody.getExpires_in();
            userServiceClient.saveCoinbaseTokens(
                    userId,
                    accessToken,
                    refreshToken,
                    expiresIn
            );

            return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "OAuth process completed successfully!",
                ""
            );
        }
        else {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Failed to OAuth. Status: "  + response.getStatusCode(),
                ""
            );
        }
    }
}
