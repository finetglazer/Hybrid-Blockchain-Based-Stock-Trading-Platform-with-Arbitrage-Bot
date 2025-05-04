package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.common.Const;
import com.project.userservice.model.ActiveSession;
import com.project.userservice.model.PasswordResetToken;
import com.project.userservice.model.User;
import com.project.userservice.model.VerificationToken;
import com.project.userservice.payload.request.client.LoginRequest;
import com.project.userservice.payload.request.client.RegisterRequest;
import com.project.userservice.repository.ActiveSessionRepository;
import com.project.userservice.repository.PasswordResetTokenRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.VerificationTokenRepository;
import com.project.userservice.security.ForgotPasswordRateLimiterService;
import com.project.userservice.security.JwtProvider;
import com.project.userservice.security.TokenBlacklistService;
import com.project.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailService;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService blacklistService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ForgotPasswordRateLimiterService rateLimiterService;
    private final ActiveSessionRepository activeSessionRepository;

    // Example: read from application.properties: "app.verification-token-expiration=24"
    @Value("${app.verification-token-expiration:24}")
    private int tokenExpirationHours;

    @Value("${resetBaseUrl}")
    private String resetBaseUrl;

    @Override
    public BaseResponse<?> register(RegisterRequest request) {
        // 1) Check if username/email is taken
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Username is already in use!",
                ""
            ); // 0 is the error code
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Email is already in use!",
                    ""
            ); // 0 is the error code
        }

        // 2) Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3) Generate random token
        String token = UUID.randomUUID().toString();

        // 4) Build VerificationToken record
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setEmail(request.getEmail());
        verificationToken.setUsername(request.getUsername());
        verificationToken.setPasswordHash(hashedPassword);
        verificationToken.setToken(token);
        verificationToken.setCreatedAt(Instant.now());
        verificationToken.setExpiresAt(Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS));

        // 5) Save to verification_tokens
        verificationTokenRepository.save(verificationToken);

        // 6) Send email (placeholder)
        // In real code, you'd use an EmailService to send a link:
        // After you create and save VerificationToken:
        emailService.sendVerificationEmail(request.getEmail(), token);

        // 7) Return success response
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "User registered successfully! Please verify your email.",
            ""
        ); // 1 is the success code
    }



    @Override
    public BaseResponse<?> verifyUser(String token) {
        // 1) Look up the token
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Invalid or expired token!",
                    ""
            );
        }

        VerificationToken verificationToken = tokenOpt.get();

        // 2) Check expiration (If using TTL, it might auto-remove. We still do a manual check.)
        if (Instant.now().isAfter(verificationToken.getExpiresAt())) {
            // Optionally, delete it
            verificationTokenRepository.delete(verificationToken);
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Token has expired",
                    ""
            );
        }

        // 3) Create a User in "users" collection
        User user = new User();
        user.setUsername(verificationToken.getUsername());
        user.setEmail(verificationToken.getEmail());
        user.setPasswordHash(verificationToken.getPasswordHash());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setPermissions(Set.of("VIEW_ONLY")); // Default permission
        userRepository.save(user);

        // 4) Delete (or rely on TTL) the verification token doc
        verificationTokenRepository.delete(verificationToken);

        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "User verified successfully!",
                ""
        );
    }

    @Override
    public BaseResponse<?> login(LoginRequest request, String ipAddress) {
        // 1) Find user by username OR email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }

        // return with baseResponse form
        if (userOpt.isEmpty()) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with: " + request.getUsernameOrEmail(),
                ""
            );
        }
        User user = userOpt.get();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 2) Compare password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Incorrect password",
                ""
            );
        }

        // 3) Check for an existing active session for this user
        Optional<ActiveSession> existingSessionOpt = activeSessionRepository.findByUserId(user.getId());
        if (existingSessionOpt.isPresent()) {
            ActiveSession existingSession = existingSessionOpt.get();
            // If the existing session's IP is different from the current one, block login
            if (!existingSession.getIpAddress().equals(ipAddress)) {
                logger.info("Concurrent login attempt for user {}: existing IP = {}, new IP = {}",
                        user.getEmail(), existingSession.getIpAddress(), ipAddress);
                // Send notification email to the user
                emailService.sendConcurrentLoginNotification(user.getEmail(), existingSession.getIpAddress(), ipAddress);
                // Return a response indicating that the account is already in use from a different location
                return new BaseResponse<>(
                    Const.STATUS_RESPONSE.ERROR,
                    "Your account is already logged in from another location. Please log out there before logging in here.",
                    ""
                );
            }
        }

        // 4) If no conflicting session, proceed with login
        String token = jwtProvider.generateToken(user.getId());
        // Create a new active session record (or update existing)
        ActiveSession session = new ActiveSession();
        session.setUserId(user.getId());
        session.setIpAddress(ipAddress);
        session.setLoginTime(Instant.now());
        session.setToken(token);
        activeSessionRepository.deleteByUserId(user.getId()); // Remove any stale session
        activeSessionRepository.save(session);

        // 5) Return the JWT and success message
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Login successful",
            token
        );
    }

    @Override
    public BaseResponse<?> logout(String token) {
        long remainingSeconds = jwtProvider.getRemainingExpiry(token);
        if (remainingSeconds > 0) {
            blacklistService.blacklistToken(token, remainingSeconds);
        }

        String userId = jwtProvider.getUserIdFromToken(token);
        activeSessionRepository.deleteByUserId(userId);
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Logout successfully",
            ""
        );
    }

    @Override
    public BaseResponse<?> changePassword(String userId, String oldPassword, String newPassword) {
        // 1) Fetch user
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found",
                ""
            );
        }

        // 2) Check old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Incorrect old password",
                ""
            );
        }

        // 3) Hash new password & update
        String newHashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHashed);
        userRepository.save(user);

        // 4) Return success
        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Password changed successfully",
                ""
        );
    }

    @Override
    public BaseResponse<?> forgotPassword(String email) {
        // Rate-limit check
//        if (!rateLimiterService.isAllowed(email)) {
//            return new BaseResponse<>(
//                    Const.STATUS_RESPONSE.ERROR,
//                    "Too many requests. Please wait before trying again",
//                    ""
//            );
//        }

        // Check if a user with this email exists
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // For security, don't reveal if email exists or not
            return new BaseResponse<>(
                    Const.STATUS_RESPONSE.SUCCESS,
                    "If an account with that email exists, a password reset link has been sent",
                    ""
            );
        }

        // Generate a unique reset token
        String resetToken = UUID.randomUUID().toString();

        // Create and save the PasswordResetToken
        PasswordResetToken prt = new PasswordResetToken();
        prt.setEmail(email);
        prt.setToken(resetToken);
        prt.setCreatedAt(Instant.now());
        prt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));  // 1 hour expiry

        passwordResetTokenRepository.save(prt);

        // Send the reset email with proper template
        emailService.sendPasswordResetEmail(email, resetToken);

        // Log the audit event
        logger.info("Audit: Password reset requested for email: {}", email);

        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "If an account with that email exists, a password reset link has been sent",
                ""
        );
    }

    @Override
    public BaseResponse<?> resetPassword(String token, String newPassword) {
        // 1) Look up the reset token in the DB
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Invalid or expired token",
                ""
            );
        }
        PasswordResetToken resetToken = tokenOpt.get();

        // 2) Check if the token is expired
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            // Delete expired token for cleanup
            passwordResetTokenRepository.delete(resetToken);
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Reset token has expired",
                ""
            );
        }

        // 3) Retrieve the user by email from the reset token
        String email = resetToken.getEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not found with this email " + email,
                ""
            );
        }
        User user = userOpt.get();

        // 4) Hash the new password and update the user's password
        String newHashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHashedPassword);
        userRepository.save(user);

        // 5) Delete the reset token to prevent reuse
        passwordResetTokenRepository.delete(resetToken);

        // 6) Log the event for audit purposes
        logger.info("Audit: Password reset successfully for email: {}", email);

        // 7) Return a success message
        return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Password reset successfully!",
                ""
        );
    }


}
