package com.project.userservice.service;

public interface SmsService {
    String generatePhoneVerificationToken(String phoneNumber);
    boolean verifyPhoneAuthCredential(String idToken);
}