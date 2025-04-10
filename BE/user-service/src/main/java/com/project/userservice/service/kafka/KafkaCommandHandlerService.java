package com.project.userservice.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.common.BaseResponse;
import com.project.userservice.model.kafka.CommandMessage;
import com.project.userservice.model.kafka.EventMessage;
import com.project.userservice.payload.response.client.GetVerificationStatusResponse;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaCommandHandlerService {

    private final UserService userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper; // Add this if not already present

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

        // Create the event object
        EventMessage event = createVerificationResponseEvent(command, isVerified, verificationStatus);

        try {

            // Instead of this:
            // String eventJson = objectMapper.writeValueAsString(event);
            // kafkaTemplate.send("user.events.verify", command.getSagaId(), eventJson);

            // Do this:
            kafkaTemplate.send("user.events.verify", command.getSagaId(), event);

            log.info("Sent USER_IDENTITY_VERIFIED response for saga: {}, verified: {}",
                    command.getSagaId(), isVerified);
        } catch (Exception e) {
            log.error("Error serializing or sending event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a verification response event
     */
    private EventMessage createVerificationResponseEvent(CommandMessage command, boolean verified,
                                                         GetVerificationStatusResponse verificationStatus) {
        EventMessage event = new EventMessage();
        event.setMessageId(UUID.randomUUID().toString());
        event.setSagaId(command.getSagaId());
        event.setStepId(command.getStepId());
        event.setType("USER_IDENTITY_VERIFIED");
        event.setTimestamp(Instant.now());
        event.setSourceService("USER_SERVICE");
        event.setSuccess(verified);

        if (verified) {
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

        return event;
    }

    /**
     * Convert EventMessage to Map for Kafka
     */
    private Map<String, Object> convertEventToMap(EventMessage event) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("messageId", event.getMessageId());
        eventMap.put("sagaId", event.getSagaId());
        eventMap.put("stepId", event.getStepId());
        eventMap.put("type", event.getType());
        eventMap.put("timestamp", event.getTimestamp());
        eventMap.put("sourceService", event.getSourceService());
        eventMap.put("success", event.getSuccess());
        eventMap.put("errorCode", event.getErrorCode());
        eventMap.put("errorMessage", event.getErrorMessage());
        eventMap.put("payload", event.getPayload());
        return eventMap;
    }
}