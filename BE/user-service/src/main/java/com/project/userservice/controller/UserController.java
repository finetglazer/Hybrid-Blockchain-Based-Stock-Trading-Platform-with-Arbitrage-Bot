package com.project.userservice.controller;

import com.project.userservice.payload.request.client.PhoneNumberUpdateRequest;
import com.project.userservice.payload.request.client.PhoneNumberVerifyRequest;
import com.project.userservice.service.UserProfileService;
import com.project.userservice.model.SecurityVerification;
import com.project.userservice.payload.request.client.Enable2FARequest;
import com.project.userservice.payload.request.client.UpdatePhoneNumberRequest;
import com.project.userservice.payload.request.internal.UpdateTradingPermissionsRequest;
import com.project.userservice.payload.request.internal.ValidateTradingPermissionRequest;
import com.project.userservice.service.TwoFactorAuthService;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("users/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;

    private final UserProfileService userProfileService;
    @GetMapping("/me/trading-permissions")
    public ResponseEntity<?> getTradingPermissions(Principal principal) {
        return ResponseEntity.ok(userService.getTradingPermissions(principal.getName()));
    }

    @PutMapping("/me/phone-number")
    public ResponseEntity<?> updatePhoneNumber(
            Principal principal,
            @RequestBody PhoneNumberUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.initiatePhoneNumberUpdate(principal.getName(), request));
    }

    @GetMapping("/me/profile/enhanced")
    public ResponseEntity<?> getEnhancedProfile(Principal principal) {
        return ResponseEntity.ok(userService.getEnhancedProfile(principal.getName()));
    }

    @GetMapping("/me/verification-status")
    public ResponseEntity<?> getVerificationStatus(Principal principal) {
        return ResponseEntity.ok(userService.getVerificationStatus(principal.getName()));
    }

    @PostMapping("/me/phone-number/verify")
    public ResponseEntity<?> verifyPhoneNumberUpdate(
            Principal principal,
            @RequestBody PhoneNumberVerifyRequest request) {
        return ResponseEntity.ok(userProfileService.verifyPhoneNumberUpdate(principal.getName(), request));
    }

    @GetMapping("/me/phone-number/get")
    public ResponseEntity<?> getPhoneNumber(Principal principal) {
        return ResponseEntity.ok(userProfileService.getPhoneNumber(principal.getName()));
    }

//    @PostMapping("/me/phone-number/verify")
//    public ResponseEntity<?> verifyPhoneNumber(Principal principal, @RequestBody Verify2FARequest verify2FARequest) {
//
//    }

    @PutMapping("/internal/users/{userId}/trading-permissions/update")
    public ResponseEntity<?> updateTradingPermissions(@PathVariable String userId, @RequestBody UpdateTradingPermissionsRequest updateTradingPermissionsRequest) {
        return ResponseEntity.ok(userService.updateTradingPermissions(userId, updateTradingPermissionsRequest));
    }

    @PostMapping("/internal/users/{userId}/validate-permission")
    public ResponseEntity<?> validateTradingPermission(@PathVariable String userId, @RequestBody ValidateTradingPermissionRequest validateTradingPermissionRequest) {
        return ResponseEntity.ok(userService.validateTradingPermission(userId, validateTradingPermissionRequest));
    }

}
