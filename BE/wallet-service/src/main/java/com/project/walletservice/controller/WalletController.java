package com.project.walletservice.controller;

import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.service.CoinbaseWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/wallets")
public class WalletController {
    private final CoinbaseWalletService coinbaseWalletService;

    @PostMapping("/create")
    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest request,
            Principal principal
    ) {
        String userId = principal.getName();
        return ResponseEntity.ok(coinbaseWalletService.createWallet(userId, request));
    }
}
