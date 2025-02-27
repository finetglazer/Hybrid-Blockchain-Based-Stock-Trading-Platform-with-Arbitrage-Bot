package com.project.userservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String message;

    // structure with message
    public LoginResponse(String message) {
        this.message = message;
    }
}