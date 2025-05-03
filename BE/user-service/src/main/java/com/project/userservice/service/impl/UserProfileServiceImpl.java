package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.model.SecurityVerification;
import com.project.userservice.model.User;
import com.project.userservice.payload.request.client.PhoneNumberUpdateRequest;
import com.project.userservice.payload.request.client.PhoneNumberVerifyRequest;
import com.project.userservice.repository.SecurityVerificationRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.SmsService;
import com.project.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final SecurityVerificationRepository securityVerificationRepository;
    private final SmsService smsService;

    @Override
    public BaseResponse<?> initiatePhoneNumberUpdate(String userId, PhoneNumberUpdateRequest request) {
        // 1. Find user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "User not found",
                    ""
            );
        }

        User user = userOpt.get();
        String phoneNumber = request.getPhoneNumber();

        // 2. Validate phone number format
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Phone number cannot be empty",
                    ""
            );
        }

        // Ensure phone number starts with + for international format
        if (!phoneNumber.startsWith("+")) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Phone number must be in international format (e.g., +84123456789)",
                    ""
            );
        }

        try {
            // 3. Generate verification token via Firebase
            String sessionInfo = smsService.generatePhoneVerificationToken(phoneNumber);

            // 4. Create verification record
            SecurityVerification verification = new SecurityVerification();
            verification.setUserId(userId);
            verification.setType(SecurityVerification.VerificationType.SMS_CODE.name());
            verification.setStatus(SecurityVerification.VerificationStatus.PENDING.name());
            verification.setCreatedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            verification.setSessionInfo(sessionInfo);

            // Store the new phone number temporarily in the deviceInfo field
            // (repurposing this field for our needs)
            verification.setDeviceInfo(phoneNumber);

            SecurityVerification savedVerification = securityVerificationRepository.save(verification);

            // 5. Return verification ID and expiration
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("verificationId", savedVerification.getId());
            responseData.put("type", "SMS_CODE");
            responseData.put("expiresAt", savedVerification.getExpiresAt());

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "Verification code sent to new phone number",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error initiating phone number update: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to send verification: " + e.getMessage(),
                    ""
            );
        }
    }

    @Override
    public BaseResponse<?> verifyPhoneNumberUpdate(String userId, PhoneNumberVerifyRequest request) {
        // 1. Find verification record
        Optional<SecurityVerification> verificationOpt = securityVerificationRepository.findByIdAndStatus(
                request.getVerificationId(),
                SecurityVerification.VerificationStatus.PENDING.name()
        );

        if (verificationOpt.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification not found or already completed",
                    ""
            );
        }

        SecurityVerification verification = verificationOpt.get();

        // 2. Check if verification belongs to requesting user
        if (!verification.getUserId().equals(userId)) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification does not belong to current user",
                    ""
            );
        }

        // 3. Check if verification is expired
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            verification.setStatus(SecurityVerification.VerificationStatus.EXPIRED.name());
            securityVerificationRepository.save(verification);

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification has expired",
                    ""
            );
        }

        // 4. Verify code with Firebase
        try {
            boolean verified = smsService.verifyPhoneAuthCredential(request.getFirebaseIdToken());
            if (!verified) {
                verification.setStatus(SecurityVerification.VerificationStatus.FAILED.name());
                securityVerificationRepository.save(verification);

                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.ERROR,
                        "Invalid verification code",
                        ""
                );
            }

            // 5. Update verification status
            verification.setStatus(SecurityVerification.VerificationStatus.COMPLETED.name());
            verification.setVerifiedAt(Instant.now());
            securityVerificationRepository.save(verification);

            // 6. Update user's phone number
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.ERROR,
                        "User not found",
                        ""
                );
            }

            User user = userOpt.get();
            // Get the new phone number from the deviceInfo field
            String newPhoneNumber = verification.getDeviceInfo();
            user.setPhoneNumber(newPhoneNumber);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

            // 7. Return success response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", user.getId());
            responseData.put("phoneNumber", user.getPhoneNumber());
            responseData.put("updatedAt", user.getUpdatedAt());

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "Phone number updated successfully",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error verifying phone number update: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to verify phone number: " + e.getMessage(),
                    ""
            );
        }
    }

    @Override
    public BaseResponse<?> getPhoneNumber(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with userId: " + userId,
                ""
            );
        }

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Retrieve user phone number successfully",
            user.getPhoneNumber()
        );
    }
}
