package com.project.userservice.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
}
