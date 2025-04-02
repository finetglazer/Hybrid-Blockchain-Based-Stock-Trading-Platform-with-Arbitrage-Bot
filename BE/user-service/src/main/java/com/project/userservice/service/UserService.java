package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.internal.UpdateTradingPermissionsRequest;
import com.project.userservice.payload.request.internal.ValidateTradingPermissionRequest;

public interface UserService {
    BaseResponse<?> getTradingPermissions(String userId);
    BaseResponse<?> getEnhancedProfile(String userId);
//    BaseResponse<?> verifyPhoneNumber(String userId, Verify2FARequest verify2FARequest);
    BaseResponse<?> updateTradingPermissions(String userId, UpdateTradingPermissionsRequest updateTradingPermissionsRequest);
    BaseResponse<?> validateTradingPermission(String userId, ValidateTradingPermissionRequest validateTradingPermissionRequest);
    BaseResponse<?> getVerificationStatus(String userId);
}
