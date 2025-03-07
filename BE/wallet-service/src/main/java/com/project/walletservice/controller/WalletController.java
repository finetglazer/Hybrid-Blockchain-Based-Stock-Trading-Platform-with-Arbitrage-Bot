package com.project.walletservice.controller;

import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.payload.request.client.CreateWalletRequest;
import com.project.walletservice.service.CoinbaseWalletService;
import com.project.walletservice.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("wallets/api/v1/coinbase-wallet")
@RequiredArgsConstructor
public class WalletController {

    private final CoinbaseWalletService coinbaseWalletService;
    private final OAuthService oAuthService;

    @PostMapping("/create")
    public BaseResponse<?> createWallet(
            @RequestBody CreateWalletRequest request,
            Principal principal  // Use standard Principal
    ) {
        // 1) Extract user ID from principal (e.g., principal.getName())
        // For example, if principal.getName() returns the user ID as a string
//         userId = principal.getName();

        String userId = "1";
        // 2) Call the service
        return coinbaseWalletService.createWallet(userId, request);
    }

    @PostMapping("/exchange-auth-code")
    public BaseResponse<?> exchangeAuthCode(
            @RequestParam String userId,
            @RequestParam String authCode
    ) {
        return oAuthService.exchangeAuthCode(userId, authCode);
    }

    @PostMapping("/test")
    public ResponseEntity<?> testWalletAPI() {
        return ResponseEntity.ok("Reached the end of /wallets/api/v1/coinbase-wallet/test API");
    }
}
