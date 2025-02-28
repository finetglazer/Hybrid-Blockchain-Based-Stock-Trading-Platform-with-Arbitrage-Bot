package com.project.userservice.service.impl;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.model.PasswordResetToken;
import com.project.userservice.model.User;
import com.project.userservice.model.VerificationToken;
import com.project.userservice.payload.request.LoginRequest;
import com.project.userservice.payload.request.RegisterRequest;
import com.project.userservice.payload.response.LoginResponse;
import com.project.userservice.payload.response.RegisterResponse;
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

    // Example: read from application.properties: "app.verification-token-expiration=24"
    @Value("${app.verification-token-expiration:24}")
    private int tokenExpirationHours;

    @Override
    public BaseResponse<?> register(RegisterRequest request) {
        // 1) Check if username/email is taken
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return new BaseResponse<>("Username is already in use!"); // 0 is the error code
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new BaseResponse<>("Email is already in use!"); // 0 is the error code
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
        return new BaseResponse<>(new RegisterResponse("User registered successfully! Please verify your email.")); // 1 is the success code
    }



    @Override
    public BaseResponse<?> verifyUser(String token) {
        // 1) Look up the token
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);
        if (!tokenOpt.isPresent()) {
            return new BaseResponse<>("Invalid or expired token.");
        }

        VerificationToken verificationToken = tokenOpt.get();

        // 2) Check expiration (If using TTL, it might auto-remove. We still do a manual check.)
        if (Instant.now().isAfter(verificationToken.getExpiresAt())) {
            // Optionally, delete it
            verificationTokenRepository.delete(verificationToken);
            return new BaseResponse<>("Token has expired.");
        }

        // 3) Create a User in "users" collection
        User user = new User();
        user.setUsername(verificationToken.getUsername());
        user.setEmail(verificationToken.getEmail());
        user.setPasswordHash(verificationToken.getPasswordHash());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // 4) Delete (or rely on TTL) the verification token doc
        verificationTokenRepository.delete(verificationToken);

        return new BaseResponse<>(new RegisterResponse("User verified successfully!"));
    }

    @Override
    public BaseResponse<?> login(LoginRequest request) {
        // 1) Find user by username OR email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }
//        User user = userOpt.orElseThrow(
//                () -> new RuntimeException("User not found with: " + request.getUsernameOrEmail())
//        );
        // return with baseRespone form
        if (!userOpt.isPresent()) {
            return new BaseResponse<>("User not found with: " + request.getUsernameOrEmail());
        }
        User user = userOpt.get();

        // 2) Compare password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return new BaseResponse<>("Incorrect password.");
        }

        // 3) Generate JWT token
        // You can store user.getId() in 'sub'
        String token = jwtProvider.generateToken(user.getId());

        // 4) Return the response
        return new BaseResponse<>(new LoginResponse(token, "Login successful!"));
    }

    @Override
    public BaseResponse<?> logout(String token) {
        long remainingSeconds = jwtProvider.getRemainingExpiry(token);
        if (remainingSeconds > 0) {
            blacklistService.blacklistToken(token, remainingSeconds);
        }
        return new BaseResponse<>("Logout successfully.");
    }

    @Override
    public BaseResponse<?> changePassword(String userId, String oldPassword, String newPassword) {
        // 1) Fetch user
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return new BaseResponse<>("User not found.");
        }

        // 2) Check old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return new BaseResponse<>("Incorrect old password.");
        }

        // 3) Hash new password & update
        String newHashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHashed);
        userRepository.save(user);

        // 4) Return success
        return new BaseResponse<>(1, null, "Password changed successfully.");
    }

    @Override
    public BaseResponse<?> forgotPassword(String email) {
        // Rate-limit check
        if (!rateLimiterService.isAllowed(email)) {
            return new BaseResponse<>("Too many requests. Please wait before trying again.");
        }

        // Check if a user with this email exists
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            // Send message no email exist
            return new BaseResponse<>("No user found with this email.");
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

        // Construct the reset link (adjust the host/port as needed)
        //testing
        String resetLink = "http://localhost:8081/api/v1/auth/reset-password?token=" + resetToken;

        // Send the reset email (here we simulate by logging the link)
        emailService.sendPasswordResetEmail(email, resetToken);
        logger.info("Password reset link for {}: {}", email, resetLink);


        // Log the audit event
        logger.info("Audit: Password reset requested for email: {}", email);

        return new BaseResponse<>(1, null, "If the email exists, a reset link has been sent.");
    }

    @Override
    public BaseResponse<?> resetPassword(String token, String newPassword) {
        // 1) Look up the reset token in the DB
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (!tokenOpt.isPresent()) {
            return new BaseResponse<>("Invalid or expired reset token.");
        }
        PasswordResetToken resetToken = tokenOpt.get();

        // 2) Check if the token is expired
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            // Delete expired token for cleanup
            passwordResetTokenRepository.delete(resetToken);
            return new BaseResponse<>("Reset token has expired.");
        }

        // 3) Retrieve the user by email from the reset token
        String email = resetToken.getEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return new BaseResponse<>("User not found for email: " + email);
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
        return new BaseResponse<>(1, null, "Password reset successfully!");
    }


}
