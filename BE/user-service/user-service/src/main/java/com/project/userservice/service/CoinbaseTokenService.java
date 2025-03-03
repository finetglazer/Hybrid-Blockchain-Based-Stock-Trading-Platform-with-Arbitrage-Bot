package com.project.userservice.service;

import com.project.userservice.model.User;
import com.project.userservice.payload.response.UserCoinbaseTokenResponse;

public interface CoinbaseTokenService {
    UserCoinbaseTokenResponse getValidToken(String userId);
}
