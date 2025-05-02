package com.project.userservice.controller;

import com.project.userservice.payload.request.client.Disable2FARequest;
import com.project.userservice.payload.request.client.Enable2FARequest;
import com.project.userservice.payload.request.client.RecoveryKeyVerifyRequest;
import com.project.userservice.payload.request.client.Verify2FARequest;
import com.project.userservice.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("users/api/v1/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/enable")
    public ResponseEntity<?> enable2FA(Principal principal, @RequestBody Enable2FARequest request) {
        return ResponseEntity.ok(twoFactorAuthService.enable2FA(principal.getName(), request));
    }

    @GetMapping("/create")
    public ResponseEntity<?> create2FA(Principal principal) {
        return ResponseEntity.ok(twoFactorAuthService.create2FA(principal.getName()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify2FA(@RequestBody Verify2FARequest request) {
        return ResponseEntity.ok(twoFactorAuthService.verify2FA(request));
    }

    @PostMapping("/recovery-keys/generate")
    public ResponseEntity<?> generateRecoveryKeys(Principal principal) {
        return ResponseEntity.ok(twoFactorAuthService.generateRecoveryKeys(principal.getName()));
    }

    @PostMapping("/recovery-keys/verify")
    public ResponseEntity<?> verifyWithRecoveryKey(Principal principal, @RequestBody RecoveryKeyVerifyRequest request) {
        return ResponseEntity.ok(twoFactorAuthService.verifyWithRecoveryKey(principal.getName(), request));
    }

    @GetMapping("/info")
    public ResponseEntity<?> get2FAInfo(Principal principal) {
        return ResponseEntity.ok(twoFactorAuthService.get2FAInfo(principal.getName()));
    }

    // Add this new endpoint for disabling 2FA
    @PostMapping("/disable")
    public ResponseEntity<?> disable2FA(Principal principal, @RequestBody Disable2FARequest request) {
        return ResponseEntity.ok(twoFactorAuthService.disable2FA(principal.getName(), request));
    }
}
