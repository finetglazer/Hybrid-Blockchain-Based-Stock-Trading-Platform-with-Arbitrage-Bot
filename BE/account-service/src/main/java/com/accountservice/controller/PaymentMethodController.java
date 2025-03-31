package com.accountservice.controller;

import com.accountservice.payload.request.client.CreatePaymentMethodRequest;
import com.accountservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("payment-methods/api/v1")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

//    @PostMapping("/me/create")
//    public ResponseEntity<?> createPaymentMethod(Principal principal, @RequestBody CreatePaymentMethodRequest createPaymentMethodRequest) {
//        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(principal.getName(), createPaymentMethodRequest));
//    }
}
