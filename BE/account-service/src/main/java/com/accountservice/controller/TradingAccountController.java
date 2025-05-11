package com.accountservice.controller;

import com.accountservice.payload.request.client.CreateTradingAccountRequest;
import com.accountservice.payload.request.client.GetBalanceHistoryRequest;
import com.accountservice.payload.request.client.GetUserAccountsRequest;
import com.accountservice.payload.request.client.UpdateTradingAccountRequest;
import com.accountservice.payload.response.internal.HasTradingAccountAndPaymentMethodResponse;
import com.accountservice.service.TradingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("accounts/api/v1")
@RequiredArgsConstructor
public class TradingAccountController {
    private final TradingAccountService tradingAccountService;

    @PostMapping("/create")
    public ResponseEntity<?> createTradingAccount(Principal principal, @RequestBody CreateTradingAccountRequest createTradingAccountRequest) {
        return ResponseEntity.ok(tradingAccountService.createTradingAccount(principal.getName(), createTradingAccountRequest));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountDetails(@PathVariable String accountId) {
        return ResponseEntity.ok(tradingAccountService.getAccountDetails(accountId));
    }
    // get the list of names of all trading account of a user
    @GetMapping("/get-names")
    public ResponseEntity<?> getUserAccountNames(Principal principal) {
        return ResponseEntity.ok(tradingAccountService.getUserAccountNames(principal.getName()));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getUserAccounts(
        Principal principal,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(tradingAccountService.getUserAccounts(
            new GetUserAccountsRequest(principal.getName(), status, page, size)
        ));
    }

    @PutMapping("/{accountId}/update")
    public ResponseEntity<?> updateTradingAccount(
            @PathVariable String accountId,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "status", required = false) String status
    ) {
        return ResponseEntity.ok(tradingAccountService.updateTradingAccount(
            new UpdateTradingAccountRequest(accountId, nickname, status)
        ));
    }

    @GetMapping("/{accountId}/balance-history")
    public ResponseEntity<?> getBalanceHistory(
        @PathVariable String accountId,
        @RequestParam(name = "startDate", required = false) String startDate,
        @RequestParam(name = "endDate", required = false) String endDate,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(tradingAccountService.getBalanceHistory(
            new GetBalanceHistoryRequest(accountId, startDate, endDate, page, size)
        ));
    }

    @PostMapping("/internal/{userId}/has-account-and-payment-method")
    public ResponseEntity<HasTradingAccountAndPaymentMethodResponse> hasAccountAndPaymentMethod(@PathVariable String userId) {
        return ResponseEntity.ok(tradingAccountService.hasAccountAndPaymentMethod(userId));
    }
}
