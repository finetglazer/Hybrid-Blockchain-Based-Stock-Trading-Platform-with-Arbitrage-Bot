package com.accountservice.controller;

import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/accounts/transactions/api/v1")
@RequiredArgsConstructor
public class TransactionHistoryController {
    private final TransactionHistoryService transactionHistoryService;

    @PostMapping("/get")
    public ResponseEntity<?> getTransactions(Principal principal, @RequestBody GetTransactionsRequest getTransactionsRequest) {
        getTransactionsRequest.setUserId(principal.getName());
        return ResponseEntity.ok(transactionHistoryService.getTransactions(getTransactionsRequest));
    }

    @GetMapping("/{transactionId}/details")
    public ResponseEntity<?> getTransactionDetails(@PathVariable String transactionId) {
        return ResponseEntity.ok(transactionHistoryService.getTransactionDetails(transactionId));
    }

    @GetMapping("/internal/get")
    public ResponseEntity<?> getTransactionsInternal(@RequestBody GetTransactionsRequest getTransactionsRequest) {
        return ResponseEntity.ok(transactionHistoryService.getTransactions(getTransactionsRequest));
    }
}
