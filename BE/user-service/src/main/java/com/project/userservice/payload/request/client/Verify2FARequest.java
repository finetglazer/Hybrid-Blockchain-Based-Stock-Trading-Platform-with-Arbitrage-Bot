package com.project.userservice.payload.request.client;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Verify2FARequest {
    @NotBlank
    private String verificationId;

    @NotBlank
    private String code;

    // Add this field to receive the Firebase ID token
    private String firebaseIdToken;
}