package com.project.userservice.payload.response.client;

import com.project.userservice.model.SecurityVerification;
import com.project.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetProfileEnhancedResponse {
    private String userId;
    private String username;
    private String email;
    private String phoneNumber;
    private User.UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Boolean twoFactorEnabled;
    private List<String> twoFactorTypes;
    private List<String> tradingPermissions;
    private Boolean hasTradingAccount;
    private Boolean hasPaymentMethods;
}
