package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.client.LoginRequest;
import com.project.userservice.payload.request.client.RegisterRequest;


public interface AuthService {
    BaseResponse<?> register(RegisterRequest request);
    BaseResponse<?> verifyUser(String token);
    BaseResponse<?> login(LoginRequest request, String ipAddress);
    BaseResponse<?> logout(String token);
    BaseResponse<?> changePassword(String userId, String oldPassword, String newPassword);
    BaseResponse<?> forgotPassword(String email);
    BaseResponse<?> resetPassword(String token, String newPassword);
}
