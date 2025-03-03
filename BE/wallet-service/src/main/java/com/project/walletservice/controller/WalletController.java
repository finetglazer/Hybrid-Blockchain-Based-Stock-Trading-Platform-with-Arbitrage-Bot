package com.project.walletservice.controller;

import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.payload.response.CreateWalletResponse;
import com.project.walletservice.service.CoinbaseWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final CoinbaseWalletService coinbaseWalletService;

    @PostMapping("/create")
    public ResponseEntity<CreateWalletResponse> createWallet(
            @RequestBody CreateWalletRequest request,
            Principal principal  // Use standard Principal
    ) {
        // 1) Extract user ID from principal (e.g., principal.getName())
        // For example, if principal.getName() returns the user ID as a string
//        String userId = principal.getName();

        String userId = "1";
        // 2) Call the service
        CreateWalletResponse response = coinbaseWalletService.createWallet(userId, request);

        // 3) Return the result
        return ResponseEntity.ok(response);
    }
}
