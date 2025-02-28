package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.LoginRequest;
import com.project.userservice.payload.request.RegisterRequest;
import com.project.userservice.payload.response.LoginResponse;
import com.project.userservice.payload.response.RegisterResponse;
import org.springframework.stereotype.Service;


public interface AuthService {
    BaseResponse<?> register(RegisterRequest request);
    BaseResponse<?> verifyUser(String token);
    BaseResponse<?> login(LoginRequest request, String ipAddress);
    BaseResponse<?> logout(String token);
    BaseResponse<?> changePassword(String userId, String oldPassword, String newPassword);
    BaseResponse<?> forgotPassword(String email);
    BaseResponse<?> resetPassword(String token, String newPassword);
}
