package com.accountservice.controller;

import com.accountservice.payload.request.client.CreatePaymentMethodRequest;
import com.accountservice.payload.request.client.ListPaymentMethodsRequest;
import com.accountservice.payload.request.client.UpdatePaymentMethodRequest;
import com.accountservice.payload.request.client.VerifyPaymentMethodRequest;
import com.accountservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/accounts/payment-methods/api/v1")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @PostMapping("/me/create")
    public ResponseEntity<?> createPaymentMethod(Principal principal, @RequestBody CreatePaymentMethodRequest createPaymentMethodRequest) {
        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(principal.getName(), createPaymentMethodRequest));
    }

    @PostMapping("/{paymentMethodId}/verify")
    public ResponseEntity<?> verifyPaymentMethod(@PathVariable String paymentMethodId, @RequestBody VerifyPaymentMethodRequest verifyPaymentMethodRequest) {
        return ResponseEntity.ok(paymentMethodService.verifyPaymentMethod(paymentMethodId, verifyPaymentMethodRequest));
    }

    @GetMapping("/me/get")
    public ResponseEntity<?> listPaymentMethods(
            Principal principal,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "statuses", required = false) List<String> statuses
    ) {
        return ResponseEntity.ok(paymentMethodService.listPaymentMethods(
            new ListPaymentMethodsRequest(principal.getName(), types, statuses)
        ));
    }

    @GetMapping("/{paymentMethodId}")
    public ResponseEntity<?> getPaymentMethodDetails(@PathVariable String paymentMethodId) {
        return ResponseEntity.ok(paymentMethodService.getPaymentMethodDetails(paymentMethodId));
    }

    @PutMapping("/{paymentMethodId}/update")
    public ResponseEntity<?> updatePaymentMethod(@PathVariable String paymentMethodId, @RequestBody UpdatePaymentMethodRequest updatePaymentMethodRequest) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(paymentMethodId, updatePaymentMethodRequest));
    }

    @DeleteMapping("/{paymentMethodId}/delete")
    public ResponseEntity<?> removePaymentMethod(@PathVariable String paymentMethodId) {
        return ResponseEntity.ok(paymentMethodService.removePaymentMethod(paymentMethodId));
    }
}
