package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.client.Disable2FARequest;
import com.project.userservice.payload.request.client.Enable2FARequest;
import com.project.userservice.payload.request.client.RecoveryKeyVerifyRequest;
import com.project.userservice.payload.request.client.Verify2FARequest;

public interface TwoFactorAuthService {
    BaseResponse<?> enable2FA(String userId, Enable2FARequest request);
    BaseResponse<?> verify2FA(Verify2FARequest request);
    BaseResponse<?> generateRecoveryKeys(String userId);
    BaseResponse<?> verifyWithRecoveryKey(String userId, RecoveryKeyVerifyRequest request);
    BaseResponse<?> create2FA(String userId);

    /**
     * Get 2FA information for the current user and prepare for disabling
     * @param userId The user ID
     * @return Response with user's 2FA details and verification session
     */
    BaseResponse<?> get2FAInfo(String userId);

    /**
     * Disable 2FA for a user after verification
     * @param userId The user ID
     * @param request The disable request containing verification details
     * @return Response indicating success or failure
     */
    BaseResponse<?> disable2FA(String userId, Disable2FARequest request);
}
