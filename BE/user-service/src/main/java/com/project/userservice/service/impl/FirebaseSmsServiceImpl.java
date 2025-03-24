package com.project.userservice.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import com.project.userservice.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FirebaseSmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseSmsServiceImpl.class);

    // For server-side SMS verification, Firebase requires direct phone verification
    // This implementation focuses on generating verification tokens that will be used
    // by the client-side SDK for actual verification

    @Override
    public String generatePhoneVerificationToken(String phoneNumber) {
        try {
            // Generate a session info token for the phone number
            // This will be sent to the client for verification

            // Note: Full server-side verification is not possible with Firebase
            // This example creates a custom token that can be used by the client

            String uid = "phone:" + phoneNumber.replace("+", "");
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);

            logger.info("Generated verification token for phone: {}", phoneNumber);

            return customToken;
        } catch (FirebaseAuthException e) {
            logger.error("Failed to generate Firebase token", e);
            throw new RuntimeException("Failed to start verification process", e);
        }
    }

    // For verification, the actual verification happens on the client side
    // This method would be used to verify the authentication result
    @Override
    public boolean verifyPhoneAuthCredential(String idToken) {
        try {
            // Verify the ID token returned from the client after verification
            FirebaseAuth.getInstance().verifyIdToken(idToken);
            return true;
        } catch (FirebaseAuthException e) {
            logger.error("Failed to verify Firebase ID token", e);
            return false;
        }
    }
}