package com.project.userservice.service.kafka;

import com.project.kafkamessagemodels.model.CommandMessage;
import com.project.kafkamessagemodels.model.EventMessage;
import com.project.userservice.common.BaseResponse;

import com.project.userservice.model.User;
import com.project.userservice.payload.response.client.GetVerificationStatusResponse;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final UserService userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepository userRepository;

    @Value("${kafka.topics.user-events.common}")
    private String userCommonEventsTopic;

    @Value("${kafka.topics.user-events.order-buy}")
    private String userOrderEventsTopic;

    @Value("${kafka.topics.user-events.order-buy}")
    private String userEventsBuyTopic;

    @Value("${kafka.topics.user-events.order-sell}")
    private String userEventsSellTopic;

    /**
     * Handle USER_VERIFY_IDENTITY command by reusing existing UserService verification logic
     */
    public void handleVerifyIdentityCommand(CommandMessage command) {
        log.info("Handling USER_VERIFY_IDENTITY command for saga: {}", command.getSagaId());

        String userId = command.getPayloadValue("userId");

        // Reuse existing verification logic from UserService
        BaseResponse<?> verificationResponse = userService.getVerificationStatus(userId);
        GetVerificationStatusResponse verificationStatus = (GetVerificationStatusResponse) verificationResponse.getData();

        // Create response event based on verification result
        boolean isVerified = verificationStatus != null &&
                "ACTIVE".equals(verificationStatus.getUserStatus()) &&
                verificationStatus.isEmailVerified();

        // Create event using the imported EventMessage from kafka-management-service
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setType("USER_IDENTITY_VERIFIED");
        event.setTimestamp(Instant.now());
        event.setSourceService("USER_SERVICE");
        event.setSuccess(isVerified);

        if (isVerified) {
            event.setPayloadValue("userId", verificationStatus.getUserId());
            event.setPayloadValue("verified", true);
            event.setPayloadValue("verificationLevel", "BASIC");
            event.setPayloadValue("emailVerified", verificationStatus.isEmailVerified());
            event.setPayloadValue("phoneVerified", verificationStatus.isPhoneVerified());
        } else {
            event.setSuccess(false);
            event.setErrorCode("USER_VERIFICATION_FAILED");
            event.setErrorMessage(verificationStatus == null ?
                    "User not found" : "User account is not active: " + verificationStatus.getUserStatus());
            event.setPayloadValue("verified", false);
        }

        try {
            kafkaTemplate.send(userCommonEventsTopic, command.getSagaId(), event);
            log.info("Sent USER_IDENTITY_VERIFIED response for saga: {}, verified: {}",
                    command.getSagaId(), isVerified);
        } catch (Exception e) {
            log.error("Error sending event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle USER_VERIFY_TRADING_PERMISSIONS command for BUY orders
     */
    public void handleVerifyTradingPermissionBuyCommand(CommandMessage command) {
        log.info("Handling USER_VERIFY_TRADING_PERMISSIONS for BUY order saga: {}", command.getSagaId());

        // Common verification logic
        verifyTradingPermission(command, userEventsBuyTopic);
    }

    /**
     * Handle USER_VERIFY_TRADING_PERMISSIONS command for SELL orders
     */
    public void handleVerifyTradingPermissionSellCommand(CommandMessage command) {
        log.info("Handling USER_VERIFY_TRADING_PERMISSIONS for SELL order saga: {}", command.getSagaId());

        // Common verification logic
        verifyTradingPermission(command, userEventsSellTopic);
    }

    /**
     * Common method for verifying trading permissions
     */
    private void verifyTradingPermission(CommandMessage command, String responseTopic) {
        String userId = command.getPayloadValue("userId");
        String orderType = command.getPayloadValue("orderType");

        // Create response event
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setType("USER_TRADING_PERMISSIONS_VERIFIED");
        event.setTimestamp(Instant.now());
        event.setSourceService("USER_SERVICE");

        try {
            // Find user by ID
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                handleTradingPermissionFailure(event, "USER_NOT_FOUND", "User not found with ID: " + userId,
                        responseTopic);
                return;
            }

            User user = userOpt.get();

            // Check if user account is active
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                handleTradingPermissionFailure(event, "USER_NOT_ACTIVE",
                        "User account is not active: " + user.getStatus(), responseTopic);
                return;
            }

            // Determine required permission based on order type
            String requiredPermission = determineRequiredPermission(orderType);

            // Check if user has the required permission
            boolean hasPermission = user.hasTradingPermission(requiredPermission);

            if (!hasPermission) {
                handleTradingPermissionFailure(event, "INSUFFICIENT_TRADING_PERMISSION",
                        "User does not have the required permission: " + requiredPermission,
                        responseTopic);
                return;
            }

            // Success case
            event.setSuccess(true);
            event.setPayloadValue("userId", userId);
            event.setPayloadValue("orderType", orderType);
            event.setPayloadValue("permissionVerified", true);
            event.setPayloadValue("permissionLevel", requiredPermission);

        } catch (Exception e) {
            log.error("Error verifying trading permissions", e);
            handleTradingPermissionFailure(event, "VERIFICATION_ERROR",
                    "Error verifying trading permissions: " + e.getMessage(), responseTopic);
            return;
        }

        // Send the response event
        try {
            kafkaTemplate.send(responseTopic, command.getSagaId(), event);
            log.info("Sent USER_TRADING_PERMISSIONS_VERIFIED response for saga: {}", command.getSagaId());
        } catch (Exception e) {
            log.error("Error sending event: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to handle trading permission verification failures
     */
    private void handleTradingPermissionFailure(EventMessage event, String errorCode, String errorMessage,
                                                String topic) {
        event.setType("USER_TRADING_PERMISSIONS_INVALID");
        event.setSuccess(false);
        event.setErrorCode(errorCode);
        event.setErrorMessage(errorMessage);
        event.setPayloadValue("permissionVerified", false);

        try {
            kafkaTemplate.send(topic, event.getSagaId(), event);
            log.info("Sent USER_TRADING_PERMISSIONS_INVALID response to {} for saga: {}",
                    topic, event.getSagaId());
        } catch (Exception e) {
            log.error("Error sending failure event: {}", e.getMessage(), e);
        }
    }

    /**
     * Determine the required permission based on order type
     */
    private String determineRequiredPermission(String orderType) {
        // Different order types might require different permission levels
        switch (orderType) {
            case "MARKET":
                return User.TradingPermission.BASIC_TRADING.name();
            case "LIMIT":
                return User.TradingPermission.BASIC_TRADING.name();
            case "STOP":
            case "STOP_LIMIT":
                return User.TradingPermission.ADVANCED_TRADING.name();
            case "TRAILING_STOP":
            case "OCO": // One-Cancels-Other
                return User.TradingPermission.PREMIUM_TRADING.name();
            default:
                return User.TradingPermission.BASIC_TRADING.name();
        }
    }
}
