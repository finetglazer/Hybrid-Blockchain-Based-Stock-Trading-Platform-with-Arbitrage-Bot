// BE/user-service/src/main/java/com/project/userservice/model/User.java

package com.project.userservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant lastLoginAt;

    // 2FA related fields
    private Boolean twoFactorEnabled = false;
    private String twoFactorType;
    private String phoneNumber;

    @Field("status")
    private UserStatus status = UserStatus.ACTIVE; // Default value

    // Change to Set<String> instead of Set<TradingPermission>
    // Change from a Set<String> to a single String
    @Field("tradingPermissions")
    private String tradingPermissions = "VIEW_ONLY"; // Default basic permission as a single string

    // Keep the enums for type safety in other parts of the code
    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        LOCKED
    }

    public enum TradingPermission {
        VIEW_ONLY,
        BASIC_TRADING,
        ADVANCED_TRADING,
        PREMIUM_TRADING
    }

    // Helper method to set a trading permission using the enum for type safety
    public void setTradingPermission(TradingPermission permission) {
        this.tradingPermissions = permission.name();
    }

    // Helper method to check if a permission matches using the enum for type safety
    public boolean hasTradingPermission(TradingPermission permission) {
        return this.tradingPermissions.equals(permission.name());
    }
}