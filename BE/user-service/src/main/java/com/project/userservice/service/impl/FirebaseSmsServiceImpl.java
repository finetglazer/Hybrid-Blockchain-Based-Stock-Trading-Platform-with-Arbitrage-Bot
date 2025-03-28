package com.project.userservice.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.project.userservice.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseSmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseSmsServiceImpl.class);

    @Override
    public String generatePhoneVerificationToken(String phoneNumber) {
        // For Firebase Phone Auth, we don't generate verification tokens on the server
        // Instead, we return a configuration object that the client will use to initialize Firebase Auth

        logger.info("Phone verification requested for: {}", phoneNumber);

        // Generate a state token for tracking this verification request
        // This isn't used for actual verification, just for tracking on our side
        String stateToken = java.util.UUID.randomUUID().toString();

        return stateToken;
    }

    @Override
    public boolean verifyPhoneAuthCredential(String idToken) {
        try {
            // Verify the ID token returned from the client after verification
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            // Check that this token has phone auth claims
            if (decodedToken.getClaims().containsKey("phone_number")) {
                String phoneNumber = (String) decodedToken.getClaims().get("phone_number");
                logger.info("Successfully verified phone: {}", phoneNumber);
                return true;
            } else {
                logger.error("Token does not contain phone number claim");
                return false;
            }
        } catch (FirebaseAuthException e) {
            logger.error("Failed to verify Firebase ID token", e);
            return false;
        }
    }
}