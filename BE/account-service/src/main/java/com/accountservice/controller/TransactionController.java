package com.accountservice.controller;

import com.accountservice.payload.request.client.GetTransactionsRequest;
import com.accountservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions/api/v1")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/get")
    public ResponseEntity<?> getTransactions(@RequestBody GetTransactionsRequest getTransactionsRequest) {
        return ResponseEntity.ok(transactionService.getTransactions(getTransactionsRequest));
    }
}
