package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.model.PaymentMethod;
import com.project.userservice.model.SecurityVerification;
import com.project.userservice.model.User;
import com.project.userservice.payload.request.internal.UpdateTradingPermissionsRequest;
import com.project.userservice.payload.request.internal.ValidateTradingPermissionRequest;
import com.project.userservice.payload.response.client.GetProfileEnhancedResponse;
import com.project.userservice.payload.response.client.GetTradingPermissionsResponse;
import com.project.userservice.payload.response.internal.ValidateTradingPermissionResponse;
import com.project.userservice.repository.PaymentMethodRepository;
import com.project.userservice.repository.SecurityVerificationRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.TwoFactorAuthService;
import com.project.userservice.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final SecurityVerificationRepository securityVerificationRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TradingAccountRepository tradingAccountRepository;

    private final TwoFactorAuthService twoFactorAuthService;

    @Override
    public BaseResponse<?> getTradingPermissions(String userId) {
        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found",
                ""
            );
        }

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Trading permissions retrieved successfully",
            new GetTradingPermissionsResponse(
                userId,
                user.getPermissions().stream().toList(),
                user.getUpdatedAt()
            )
        );
    }

    @Override
    public BaseResponse<?> getEnhancedProfile(String userId) {
        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with userId: " + userId,
                ""                      
            );
        }

        SecurityVerification securityVerification = securityVerificationRepository.findByUserId(userId).orElse(null);
        if (securityVerification == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Security verification not found for userId: " + userId,
                ""
            );
        }
        
        TradingAccount tradingAccount = tradingAccountRepository.findByUserId(userId).orElse(null);
        PaymentMethod paymentMethod = paymentMethodRepository.findByUserId(userId).orElse(null);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Enhanced profile retrieved successfully",
            new GetProfileEnhancedResponse(
                userId,
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getTwoFactorEnabled(),
                securityVerification.getType(),
                user.getPermissions().stream().toList(),
                tradingAccount != null,
                paymentMethod != null
            )
        );
    }

//    @Override
//    public BaseResponse<?> verifyPhoneNumber(String userId, Verify2FARequest verify2FARequest) {
//        BaseResponse<?> verify2FAResponse = twoFactorAuthService.verify2FA(verify2FARequest);
//        if (verify2FAResponse.getStatus().equals(Const.STATUS_RESPONSE.ERROR)) {
//            return verify2FAResponse;
//        }
//        return new BaseResponse<>(
//            Const.STATUS_RESPONSE.SUCCESS,
//            "Phone number updated successfully",
//            new VerifyPhoneNumberUpdateResponse(
//                userId,
//
//            )
//        )
//    }

    @Override
    public BaseResponse<?> updateTradingPermissions(String userId, UpdateTradingPermissionsRequest updateTradingPermissionsRequest) {
        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with userId: " + userId,
                ""
            );
        }

        Set<String> permissions = user.getPermissions();
        permissions.addAll(updateTradingPermissionsRequest.getPermissions());
        user.setPermissions(permissions);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Trading permissions updated successfully",
            new GetTradingPermissionsResponse(
                userId,
                user.getPermissions().stream().toList(),
                user.getUpdatedAt()
            )
        );
    }

    @Override
    public BaseResponse<?> validateTradingPermission(String userId, ValidateTradingPermissionRequest validateTradingPermissionRequest) {
        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with userId: " + userId,
                    ""
            );
        }

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Permission validation successful",
            new ValidateTradingPermissionResponse(
                userId,
                user.hasTradingPermission(validateTradingPermissionRequest.getRequiredPermission()),
                user.getPermissions().stream().toList(),
                validateTradingPermissionRequest.getRequiredPermission()
            )
        );
    }
}
