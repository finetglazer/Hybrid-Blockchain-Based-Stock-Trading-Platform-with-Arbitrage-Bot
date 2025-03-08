package com.project.userservice.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
    void sendPasswordResetEmail(String toEmail, String token);
    void sendConcurrentLoginNotification(String toEmail, String oldIp, String newIp);
}
