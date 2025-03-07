package com.project.userservice.payload.request.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest {
    private String usernameOrEmail;
    private String password;
}
