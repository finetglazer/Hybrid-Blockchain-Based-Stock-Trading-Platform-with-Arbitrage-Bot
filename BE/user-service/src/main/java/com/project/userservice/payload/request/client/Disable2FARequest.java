package com.project.userservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Disable2FARequest {
    private String verificationId;
    private String firebaseIdToken;
    private String verificationMethod;
    private String recoveryKeyVerificationId;
}