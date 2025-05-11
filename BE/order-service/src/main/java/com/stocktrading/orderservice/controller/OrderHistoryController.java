package com.stocktrading.orderservice.controller;

import com.stocktrading.orderservice.payload.request.GetOrderRequest;
import com.stocktrading.orderservice.service.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("orders/api/v1")
@RequiredArgsConstructor
public class OrderHistoryController {
    private final OrderHistoryService orderHistoryService;

    @PostMapping("/get")
    public ResponseEntity<?> getOrders(Principal principal, @RequestBody GetOrderRequest getOrderRequest) {
        getOrderRequest.setUserId(principal.getName());
        return ResponseEntity.ok(orderHistoryService.getOrders(getOrderRequest));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable String orderId) {
        return ResponseEntity.ok(orderHistoryService.getOrderDetails(orderId));
    }
}
