package com.project.userservice.service.impl;

import com.project.userservice.payload.response.UserCoinbaseTokenResponse;
import com.project.userservice.model.User;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.CoinbaseTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoinbaseTokenServiceImpl implements CoinbaseTokenService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    // Values you'd configure for your Coinbase app
//    @Value("${coinbase.clientId}")
//    private String clientId;
//
//    @Value("${coinbase.clientSecret}")
//    private String clientSecret;

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
//        if (isExpired(user)) {
//            // Attempt to refresh
//            boolean success = refreshToken(user);
//            if (!success) {
//                response.setLinked(false);
//                response.setErrorMessage("Failed to refresh token. Possibly revoked.");
//                return response;
//            }
//            // If refresh succeeds, user is updated in DB with new tokens
//        }

        // 3) Return success
        response.setLinked(true);
        response.setAccessToken(user.getCoinbaseAccessToken());
        response.setExpiresAt(user.getCoinbaseTokenExpiresAt());
        return response;
    }

    private boolean isExpired(User user) {
        Instant expiresAt = user.getCoinbaseTokenExpiresAt();
        if (expiresAt == null) return true; // no expiry means we treat as expired
        // If current time is after expiresAt, it's expired
        return Instant.now().isAfter(expiresAt);
    }

//    private boolean refreshToken(User user) {
//        try {
//            // 1) Build request to Coinbase token endpoint
//            // (Check Coinbase docs for the exact refresh URL)
//            String url = "https://api.coinbase.com/oauth/token";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            // 2) Build the form data
//            Map<String, String> body = new HashMap<>();
//            body.put("grant_type", "refresh_token");
//            body.put("refresh_token", user.getCoinbaseRefreshToken());
//            body.put("client_id", clientId);
//            body.put("client_secret", clientSecret);
//
//            // Convert map to the application/x-www-form-urlencoded format
//            String formData = buildFormData(body);
//
//            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
//
//            // 3) Call the endpoint
//            ResponseEntity<RefreshResponse> coinbaseResp =
//                    restTemplate.exchange(url, HttpMethod.POST, entity, RefreshResponse.class);
//
//            if (!coinbaseResp.getStatusCode().is2xxSuccessful() ||
//                    coinbaseResp.getBody() == null) {
//                return false;
//            }
//
//            // 4) Parse the new tokens
//            RefreshResponse resp = coinbaseResp.getBody();
//
//            // Coinbase typically returns "access_token", "refresh_token", "expires_in" (seconds)
//            user.setCoinbaseAccessToken(resp.getAccessToken());
//            user.setCoinbaseRefreshToken(resp.getRefreshToken());
//            // compute new expiry
//            Instant newExpiry = Instant.now().plusSeconds(resp.getExpiresIn());
//            user.setCoinbaseTokenExpiresAt(newExpiry);
//
//            // 5) Save user
//            userRepository.save(user);
//
//            return true;
//        } catch (RestClientException ex) {
//            return false;
//        }
//    }

    // Utility to build x-www-form-urlencoded body from map
    private String buildFormData(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    // A small DTO for parsing Coinbase refresh response
    private static class RefreshResponse {
        private String access_token;
        private String refresh_token;
        private long expires_in;

        public String getAccessToken() {
            return access_token;
        }
        public String getRefreshToken() {
            return refresh_token;
        }
        public long getExpiresIn() {
            return expires_in;
        }
    }
}
