package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.model.RecoveryKey;
import com.project.userservice.model.SecurityVerification;
import com.project.userservice.model.User;
import com.project.userservice.payload.request.client.Disable2FARequest;
import com.project.userservice.payload.request.client.Enable2FARequest;
import com.project.userservice.payload.request.client.RecoveryKeyVerifyRequest;
import com.project.userservice.payload.request.client.Verify2FARequest;
import com.project.userservice.repository.RecoveryKeyRepository;
import com.project.userservice.repository.SecurityVerificationRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.SmsService;
import com.project.userservice.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthServiceImpl.class);

    private final UserRepository userRepository;
    private final SecurityVerificationRepository securityVerificationRepository;
    private final RecoveryKeyRepository recoveryKeyRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public BaseResponse<?> enable2FA(String userId, Enable2FARequest request) {
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

        // 2. Validate phone number format
        if (!request.getPhoneNumber().startsWith("+")) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Phone number must be in international format (e.g., +84123456789)",
                    ""
            );
        }

        try {
            // 3. Generate verification token via Firebase
            String sessionInfo = smsService.generatePhoneVerificationToken(request.getPhoneNumber());

            // 4. Create verification record
            SecurityVerification verification = new SecurityVerification();
            verification.setUserId(userId);
            verification.setType(SecurityVerification.VerificationType.valueOf(request.getType()).name());
            verification.setStatus(SecurityVerification.VerificationStatus.PENDING.name());
            verification.setCreatedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            verification.setSessionInfo(sessionInfo);

            SecurityVerification savedVerification = securityVerificationRepository.save(verification);

            // 5. Update user's phone number
            user.setPhoneNumber(request.getPhoneNumber());
            userRepository.save(user);

            // 6. Return verification ID and expiration
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("verificationId", savedVerification.getId());
            responseData.put("type", request.getType());
            responseData.put("expiresAt", savedVerification.getExpiresAt());

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "Verification code sent to phone number",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error enabling 2FA: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to enable 2FA: " + e.getMessage(),
                    ""
            );
        }
    }

    @Override
    public BaseResponse<?> verify2FA(Verify2FARequest request) {
        // 1. Find verification record
        Optional<SecurityVerification> verificationOpt = securityVerificationRepository.findById(request.getVerificationId());
        if (verificationOpt.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification not found",
                    ""
            );
        }

        SecurityVerification verification = verificationOpt.get();

        // 2. Check if verification is still valid
        if (!verification.getStatus().equals(SecurityVerification.VerificationStatus.PENDING.name())) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification is no longer pending",
                    ""
            );
        }

        if (verification.getExpiresAt().isBefore(Instant.now())) {
            verification.setStatus(SecurityVerification.VerificationStatus.EXPIRED.name());
            securityVerificationRepository.save(verification);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Verification has expired",
                    ""
            );
        }

        // 3. Verify code with Firebase
        try {
            boolean verified = smsService.verifyPhoneAuthCredential(request.getFirebaseIdToken());
            if (!verified) {
                verification.setStatus(SecurityVerification.VerificationStatus.FAILED.name());
                securityVerificationRepository.save(verification);
                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.ERROR,
                        "Invalid verification token",
                        ""
                );
            }

            // 4. Update verification status
            verification.setStatus(SecurityVerification.VerificationStatus.COMPLETED.name());
            verification.setVerifiedAt(Instant.now());
            securityVerificationRepository.save(verification);

            // 5. Update user with 2FA enabled
            Optional<User> userOpt = userRepository.findById(verification.getUserId());
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.ERROR,
                        "User not found",
                        ""
                );
            }

            User user = userOpt.get();
            user.setTwoFactorEnabled(true);
            user.setTwoFactorType(verification.getType());
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

            // 6. Generate recovery keys
            List<String> recoveryKeys = generateAndStoreRecoveryKeys(user.getId());

            // 7. Return success with recovery keys
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", user.getId());
            responseData.put("twoFactorEnabled", true);
            responseData.put("twoFactorType", user.getTwoFactorType());
            responseData.put("updatedAt", user.getUpdatedAt());
            responseData.put("recoveryKeys", recoveryKeys);

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "Two-factor authentication enabled successfully",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error verifying 2FA: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to verify 2FA: " + e.getMessage(),
                    ""
            );
        }
    }

    @Override
    public BaseResponse<?> generateRecoveryKeys(String userId) {
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

        // 2. Check if 2FA is enabled
        if (Boolean.FALSE.equals(user.getTwoFactorEnabled())) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Two-factor authentication is not enabled for this user",
                    ""
            );
        }

        // 3. Generate new recovery keys
        List<String> recoveryKeys = generateAndStoreRecoveryKeys(user.getId());

        // 4. Return success with recovery keys
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", user.getId());
        responseData.put("recoveryKeys", recoveryKeys);
        responseData.put("generatedAt", Instant.now());

        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Recovery keys generated successfully",
                responseData
        );
    }

    @Override
    public BaseResponse<?> verifyWithRecoveryKey(String userId, RecoveryKeyVerifyRequest request) {
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

        // 2. Check if 2FA is enabled
        if (Boolean.FALSE.equals(user.getTwoFactorEnabled())) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Two-factor authentication is not enabled for this user",
                    ""
            );
        }

        // 3. Verify recovery key
        List<RecoveryKey> recoveryKeys = recoveryKeyRepository.findByUserIdAndUsed(userId, false);

        boolean keyVerified = false;
        RecoveryKey usedKey = null;

        for (RecoveryKey key : recoveryKeys) {
            if (passwordEncoder.matches(request.getRecoveryKey(), key.getKeyHash())) {
                keyVerified = true;
                usedKey = key;
                break;
            }
        }

        if (!keyVerified || usedKey == null) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Invalid recovery key",
                    ""
            );
        }

        // 4. Mark recovery key as used
        usedKey.setUsed(true);
        usedKey.setUsedAt(Instant.now());
        recoveryKeyRepository.save(usedKey);

        // 5. Return success response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", user.getId());
        responseData.put("verified", true);
        responseData.put("verifiedAt", Instant.now());
        responseData.put("remainingRecoveryKeys", recoveryKeys.size() - 1);

        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Recovery key verification successful",
                responseData
        );
    }

    @Override
    public BaseResponse<?> create2FA(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "User not found with userId: " + userId,
                    ""
            );
        }
        try {
            String sessionInfo = smsService.generatePhoneVerificationToken(user.getPhoneNumber());

            SecurityVerification verification = new SecurityVerification();
            verification.setUserId(userId);
            verification.setType(SecurityVerification.VerificationType.valueOf("SMS_CODE").name());
            verification.setStatus(SecurityVerification.VerificationStatus.PENDING.name());
            verification.setCreatedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            verification.setSessionInfo(sessionInfo);

            SecurityVerification savedVerification = securityVerificationRepository.save(verification);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("verificationId", savedVerification.getId());
            responseData.put("type", verification.getType());
            responseData.put("expiresAt", savedVerification.getExpiresAt());

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "Verification code sent to phone number",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error creating 2FA: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to create 2FA: " + e.getMessage(),
                    ""
            );
        }
    }

    // Helper method to generate and store recovery keys
    private List<String> generateAndStoreRecoveryKeys(String userId) {
        // 1. Generate recovery keys (random alphanumeric strings)
        List<String> recoveryKeys = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // Generate 5 recovery keys
            String key = generateRecoveryKey();
            recoveryKeys.add(key);

            // 2. Store hashed keys
            RecoveryKey recoveryKey = new RecoveryKey();
            recoveryKey.setUserId(userId);
            recoveryKey.setKeyHash(passwordEncoder.encode(key));
            recoveryKey.setUsed(false);
            recoveryKey.setCreatedAt(Instant.now());

            recoveryKeyRepository.save(recoveryKey);
        }

        return recoveryKeys;
    }

    // Helper method to generate a single recovery key
    private String generateRecoveryKey() {
        // Generate a random alphanumeric string of the form XXXX-XXXX-XXXX-XXXX
        StringBuilder key = new StringBuilder();
        Random random = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                key.append(chars.charAt(random.nextInt(chars.length())));
            }
            if (i < 3) {
                key.append("-");
            }
        }

        return key.toString();
    }

    @Override
    public BaseResponse<?> get2FAInfo(String userId) {
        // 1. Find user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "User not found",
                    "USER_NOT_FOUND"
            );
        }

        User user = userOpt.get();

        // 2. Check if 2FA is enabled
        if (Boolean.FALSE.equals(user.getTwoFactorEnabled())) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Two-factor authentication is not enabled for this user",
                    "NOT_ENABLED"
            );
        }

        try {
            // 3. Create verification record
            SecurityVerification verification = new SecurityVerification();
            verification.setUserId(userId);
            verification.setType(SecurityVerification.VerificationType.valueOf(user.getTwoFactorType()).name());
            verification.setStatus(SecurityVerification.VerificationStatus.PENDING.name());
            verification.setCreatedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

            // Generate a session token
            String sessionInfo = java.util.UUID.randomUUID().toString();
            verification.setSessionInfo(sessionInfo);

            SecurityVerification savedVerification = securityVerificationRepository.save(verification);

            // 4. Prepare the phone number (both full and masked versions)
            String phoneNumber = user.getPhoneNumber();
            String maskedPhoneNumber = maskPhoneNumber(phoneNumber);

            // 5. Return verification ID, both phone versions, and expiration
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("verificationId", savedVerification.getId());
            responseData.put("phoneNumber", phoneNumber);  // Full phone number for Firebase
            responseData.put("maskedPhoneNumber", maskedPhoneNumber);  // Masked for display
            responseData.put("expiresAt", savedVerification.getExpiresAt());

            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "2FA information retrieved successfully",
                    responseData
            );

        } catch (Exception e) {
            logger.error("Error getting 2FA info: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to retrieve 2FA information: " + e.getMessage(),
                    ""
            );
        }
    }

    // Helper method to mask phone number for privacy
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        // Keep the first 3 digits and last 4 digits visible
        int visiblePrefix = 3;
        int visibleSuffix = 4;

        StringBuilder masked = new StringBuilder();
        masked.append(phoneNumber.substring(0, visiblePrefix));

        for (int i = visiblePrefix; i < phoneNumber.length() - visibleSuffix; i++) {
            masked.append("*");
        }

        masked.append(phoneNumber.substring(phoneNumber.length() - visibleSuffix));

        return masked.toString();
    }

    @Override
    public BaseResponse<?> disable2FA(String userId, Disable2FARequest request) {
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

        // 2. Check if 2FA is enabled
        if (Boolean.FALSE.equals(user.getTwoFactorEnabled())) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Two-factor authentication is not enabled for this user",
                    ""
            );
        }

        try {
            boolean verified = false;

            // Handle different verification methods
            if ("RECOVERY_KEY".equals(request.getVerificationMethod())) {
                // Verify with recovery key verification ID
                Optional<SecurityVerification> recoveryVerification =
                        securityVerificationRepository.findById(request.getRecoveryKeyVerificationId());

                if (recoveryVerification.isPresent() &&
                        recoveryVerification.get().getStatus().equals(SecurityVerification.VerificationStatus.COMPLETED.name()) &&
                        recoveryVerification.get().getUserId().equals(userId)) {
                    verified = true;
                }
            } else {
                // Default: Verify with Firebase token
                // 1. Find verification record
                Optional<SecurityVerification> verificationOpt =
                        securityVerificationRepository.findById(request.getVerificationId());

                if (verificationOpt.isEmpty()) {
                    return new BaseResponse<>(
                            Const.STATUS_RESPONSE.ERROR,
                            "Verification not found",
                            ""
                    );
                }

                SecurityVerification verification = verificationOpt.get();

                // 2. Check if verification is still valid
                if (!verification.getStatus().equals( SecurityVerification.VerificationStatus.PENDING.name())) {
                    return new BaseResponse<>(
                            Const.STATUS_RESPONSE.ERROR,
                            "Verification is no longer pending",
                            ""
                    );
                }

                if (verification.getExpiresAt().isBefore(Instant.now())) {
                    verification.setStatus(SecurityVerification.VerificationStatus.EXPIRED.name());
                    securityVerificationRepository.save(verification);
                    return new BaseResponse<>(
                            Const.STATUS_RESPONSE.ERROR,
                            "Verification has expired",
                            ""
                    );
                }

                // 3. Verify with Firebase
                verified = smsService.verifyPhoneAuthCredential(request.getFirebaseIdToken());
                if (verified) {
                    // Update verification status
                    verification.setStatus(SecurityVerification.VerificationStatus.COMPLETED.name());
                    verification.setVerifiedAt(Instant.now());
                    securityVerificationRepository.save(verification);
                } else {
                    verification.setStatus(SecurityVerification.VerificationStatus.FAILED.name());
                    securityVerificationRepository.save(verification);
                    return new BaseResponse<>(
                            Const.STATUS_RESPONSE.ERROR,
                            "Invalid verification token",
                            ""
                    );
                }
            }

            if (verified) {
                // Disable 2FA for the user
                user.setTwoFactorEnabled(false);
                user.setTwoFactorType(null);
                user.setUpdatedAt(Instant.now());
                userRepository.save(user);

                // Return success
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("userId", user.getId());
                responseData.put("twoFactorEnabled", false);
                responseData.put("updatedAt", user.getUpdatedAt());

                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.SUCCESS,
                        "Two-factor authentication disabled successfully",
                        responseData
                );
            } else {
                return new BaseResponse<>(
                        Const.STATUS_RESPONSE.ERROR,
                        "Verification failed",
                        ""
                );
            }
        } catch (Exception e) {
            logger.error("Error disabling 2FA: ", e);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Failed to disable 2FA: " + e.getMessage(),
                    ""
            );
        }
    }

}
