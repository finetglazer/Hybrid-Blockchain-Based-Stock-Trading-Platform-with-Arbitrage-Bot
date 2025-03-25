package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.client.Enable2FARequest;
import com.project.userservice.payload.request.client.RecoveryKeyVerifyRequest;
import com.project.userservice.payload.request.client.Verify2FARequest;

public interface TwoFactorAuthService {
    BaseResponse<?> enable2FA(String userId, Enable2FARequest request);
    BaseResponse<?> verify2FA(Verify2FARequest request);
    BaseResponse<?> generateRecoveryKeys(String userId);
    BaseResponse<?> verifyWithRecoveryKey(String userId, RecoveryKeyVerifyRequest request);
}