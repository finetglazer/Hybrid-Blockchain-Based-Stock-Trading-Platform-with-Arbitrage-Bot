package com.project.userservice.payload.request.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
