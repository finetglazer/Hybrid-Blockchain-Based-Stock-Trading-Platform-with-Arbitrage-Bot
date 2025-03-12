package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.payload.request.internal.SaveCoinbaseTokensRequest;
import com.project.userservice.payload.response.coinbase.CoinbaseApiRefreshResponse;
import com.project.userservice.payload.response.internal.UserCoinbaseTokenResponse;
import com.project.userservice.model.User;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.CoinbaseTokenService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoinbaseTokenServiceImpl implements CoinbaseTokenService {
    Dotenv dotenv = Dotenv.configure().load();

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Override
    public UserCoinbaseTokenResponse getValidToken(String userId) {
        // 1) Lookup user
        User user = userRepository.findById(userId).orElse(null);

        UserCoinbaseTokenResponse response = new UserCoinbaseTokenResponse();
        if (user == null) {
            response.setLinked(false);
            response.setErrorMessage("User not found");
            response.setAccessToken(null);
            return response;
        }
        if (user.getCoinbaseAccessToken() == null || user.getCoinbaseRefreshToken() == null) {
            // user not linked to Coinbase
            response.setLinked(false);
            response.setErrorMessage("No Coinbase tokens found. User not linked.");
            response.setAccessToken(null);
            return response;
        }

        // 2) Check if token is expired
        if (isExpired(user)) {
            // Attempt to refresh
            boolean success = refreshToken(user);
            if (!success) {         // Possibly refresh token also expired
                response.setLinked(false);
                response.setErrorMessage("Failed to refresh token. Possibly revoked.");
                return response;
            }
            // If refresh succeeds, user is updated in DB with new tokens
        }

        // 3) Return success
        response.setLinked(true);
        response.setAccessToken(user.getCoinbaseAccessToken());
        response.setExpiresAt(user.getCoinbaseTokenExpiresAt());
        return response;
    }

    @Override
    public BaseResponse<?> saveCoinbaseTokens(SaveCoinbaseTokensRequest request) {
        String userId = request.getUserId();
        String accessToken = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        Long expiresIn = request.getExpiresIn();

        // 1) Lookup user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found",
                ""
            );
        }

        // 2) Set user tokens and save
        user.setCoinbaseAccessToken(accessToken);
        user.setCoinbaseRefreshToken(refreshToken);
        user.setCoinbaseTokenExpiresAt(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
        "Save tokens successfully!",
        ""
        );
    }

    private boolean isExpired(User user) {
        Instant expiresAt = user.getCoinbaseTokenExpiresAt();
        if (expiresAt == null) return true; // no expiry means we treat as expired
        // If current time is after expiresAt, it's expired
        return Instant.now().isAfter(expiresAt);
    }

    private boolean refreshToken(User user) {
        try {
            // 1) Build request to Coinbase token endpoint
            // (Check https://docs.cdp.coinbase.com/coinbase-app/docs/coinbase-app-integration for the exact refresh URL)
            String url = "https://login.coinbase.com/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2) Build the form data
            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "refresh_token");
            body.put("refresh_token", user.getCoinbaseRefreshToken());
            body.put("client_id", dotenv.get("COINBASE_OAUTH_CLIENT_ID"));
            body.put("client_secret", dotenv.get("COINBASE_OAUTH_CLIENT_SECRET"));

            // Convert map to the application/x-www-form-urlencoded format
            String formData = buildFormData(body);

            HttpEntity<String> entity = new HttpEntity<>(formData, headers);

            // 3) Call the endpoint
            ResponseEntity<CoinbaseApiRefreshResponse> coinbaseResp =
                    restTemplate.exchange(url, HttpMethod.POST, entity, CoinbaseApiRefreshResponse.class);

            if (!coinbaseResp.getStatusCode().is2xxSuccessful() ||
                    coinbaseResp.getBody() == null) {
                return false;
            }

            // 4) Parse the new tokens
            CoinbaseApiRefreshResponse resp = coinbaseResp.getBody();

//            Coinbase typically returns
//            {
//                "access_token": "...",
//                    "token_type": "bearer",
//                    "expires_in": 3600, in seconds
//                    "refresh_token": "...",
//                    "scope": "all"
//            }
            user.setCoinbaseAccessToken(resp.getAccess_token());
            user.setCoinbaseRefreshToken(resp.getRefresh_token());
            // compute new expiry
            Instant newExpiry = Instant.now().plusSeconds(resp.getExpires_in());
            user.setCoinbaseTokenExpiresAt(newExpiry);

            // 5) Save user
            userRepository.save(user);

            return true;
        } catch (RestClientException ex) {
            return false;
        }
    }

    // Utility to build x-www-form-urlencoded body from map
    private String buildFormData(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

}
