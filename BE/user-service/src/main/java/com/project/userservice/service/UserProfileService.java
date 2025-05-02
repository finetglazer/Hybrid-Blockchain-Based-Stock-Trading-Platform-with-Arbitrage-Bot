package com.project.userservice.service;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.client.PhoneNumberUpdateRequest;
import com.project.userservice.payload.request.client.PhoneNumberVerifyRequest;

public interface UserProfileService {
    /**
     * Initiates the process of updating a user's phone number.
     * This sends a verification code to the new phone number.
     *
     * @param userId The ID of the authenticated user
     * @param request The request containing the new phone number
     * @return A response containing verification details
     */
    BaseResponse<?> initiatePhoneNumberUpdate(String userId, PhoneNumberUpdateRequest request);

    /**
     * Verifies the code sent to the new phone number and completes the update.
     *
     * @param userId The ID of the authenticated user
     * @param request The request containing verification details
     * @return A response indicating success or failure
     */
    BaseResponse<?> verifyPhoneNumberUpdate(String userId, PhoneNumberVerifyRequest request);

    BaseResponse<?> getPhoneNumber(String userId);
}
