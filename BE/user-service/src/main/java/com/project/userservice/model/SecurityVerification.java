package com.project.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "security_verifications")
public class SecurityVerification {
    @Id
    private String id;

    private String userId;

    @Field("verificationType")
    private String type;
    private String status;

    private Instant createdAt = Instant.now();
    private Instant expiresAt;
    private Instant verifiedAt;

    private String ipAddress;
    private String deviceInfo;

    // For Firebase Phone Authentication
    private String sessionInfo;  // Firebase session info for verification

    public enum VerificationType {
        SMS_CODE,
        EMAIL_CODE,
        AUTHENTICATOR_APP,
        BIOMETRIC
    }

    public enum VerificationStatus {
        PENDING,
        COMPLETED,
        EXPIRED,
        FAILED
    }
}
