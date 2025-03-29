package com.project.userservice.payload.response.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPhoneNumberUpdateResponse {
    private String userId;
    private String phoneNumber;
    private Instant updatedAt;
}
