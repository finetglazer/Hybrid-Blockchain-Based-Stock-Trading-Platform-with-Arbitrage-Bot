package com.project.userservice.controller;

import com.project.userservice.payload.request.client.PhoneNumberUpdateRequest;
import com.project.userservice.payload.request.client.PhoneNumberVerifyRequest;
import com.project.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("users/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @PutMapping("/phone-number")
    public ResponseEntity<?> updatePhoneNumber(
            Principal principal,
            @RequestBody PhoneNumberUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.initiatePhoneNumberUpdate(  principal.getName(), request));
    }

    @PostMapping("/phone-number/verify")
    public ResponseEntity<?> verifyPhoneNumberUpdate(
            Principal principal,
            @RequestBody PhoneNumberVerifyRequest request) {
        return ResponseEntity.ok(userProfileService.verifyPhoneNumberUpdate(principal.getName(), request));
    }
}