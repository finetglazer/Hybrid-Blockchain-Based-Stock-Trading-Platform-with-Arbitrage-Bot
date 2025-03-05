package com.project.walletservice.controller;

import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.payload.response.CreateWalletResponse;
import com.project.walletservice.service.CoinbaseWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("wallets/api/v1/coinbase-wallet")
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
//         userId = principal.getName();

        String userId = "1";
        // 2) Call the service
        CreateWalletResponse response = coinbaseWalletService.createWallet(userId, request);

        // 3) Return the result
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    public ResponseEntity<?> testWalletAPI() {
        return ResponseEntity.ok("Reached the end of /wallets/api/v1/coinbase-wallet/test API");
    }
}
