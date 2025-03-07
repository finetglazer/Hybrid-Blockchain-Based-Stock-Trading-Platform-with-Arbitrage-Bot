package com.project.userservice.payload.response.client;

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