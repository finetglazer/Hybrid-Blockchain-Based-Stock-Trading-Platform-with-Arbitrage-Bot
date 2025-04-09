package com.project.userservice.payload.request.client;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneNumberVerifyRequest {
    @NotBlank
    private String verificationId;

    @NotBlank
    private String code;

    // For Firebase verification
    private String firebaseIdToken;
}