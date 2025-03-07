package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.internal.SaveCoinbaseTokensRequest;
import com.project.userservice.payload.response.internal.UserCoinbaseTokenResponse;

public interface CoinbaseTokenService {
    UserCoinbaseTokenResponse getValidToken(String userId);
    BaseResponse<?> saveCoinbaseTokens(SaveCoinbaseTokensRequest request);
}
