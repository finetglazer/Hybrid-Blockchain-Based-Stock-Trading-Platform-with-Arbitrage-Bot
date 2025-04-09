package com.project.userservice.service.impl;

import com.project.userservice.client.AccountServiceClient;
import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.model.SecurityVerification;
import com.project.userservice.model.User;
import com.project.userservice.payload.request.internal.UpdateTradingPermissionsRequest;
import com.project.userservice.payload.request.internal.ValidateTradingPermissionRequest;
import com.project.userservice.payload.response.client.GetProfileEnhancedResponse;
import com.project.userservice.payload.response.client.GetTradingPermissionsResponse;
import com.project.userservice.payload.response.client.GetVerificationStatusResponse;
import com.project.userservice.payload.response.internal.HasTradingAccountAndPaymentMethodResponse;
import com.project.userservice.payload.response.internal.ValidateTradingPermissionResponse;
import com.project.userservice.repository.SecurityVerificationRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final SecurityVerificationRepository securityVerificationRepository;

    private final MongoTemplate mongoTemplate;

    private final AccountServiceClient accountServiceClient;

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

        List<SecurityVerification> securityVerifications = securityVerificationRepository.findSecurityVerificationsByUserId(userId);
        
        BaseResponse<HasTradingAccountAndPaymentMethodResponse> response = accountServiceClient.hasTradingAccountAndPaymentMethod(userId);

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
                securityVerifications.stream().map(SecurityVerification::getType).collect(Collectors.toSet()).stream().toList(),
                user.getPermissions().stream().toList(),
                response.getData().isHasTradingAccount(),
                response.getData().isHasPaymentMethods()
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

    @Override
    public BaseResponse<?> getVerificationStatus(String userId) {
        User user = userRepository.findUserById(userId).orElse(null);

        // If user found, it means email verified, if not, email is not verified
        // Check whether phone number is verified or not
        SecurityVerification securityVerification = mongoTemplate.findOne(new Query(Criteria.where("userId").is(userId)
                .and("type").is(SecurityVerification.VerificationType.SMS_CODE.name())), SecurityVerification.class);

        GetVerificationStatusResponse getVerificationStatusResponse = new GetVerificationStatusResponse();
        getVerificationStatusResponse.setUserId(userId);
        getVerificationStatusResponse.setUserStatus(user == null ? "NOT_FOUND" : user.getStatus().name());
        getVerificationStatusResponse.setEmailVerified(user != null);
        getVerificationStatusResponse.setPhoneVerified(securityVerification != null);
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Retrieved user verification status successfully",
            getVerificationStatusResponse
        );
    }
}
