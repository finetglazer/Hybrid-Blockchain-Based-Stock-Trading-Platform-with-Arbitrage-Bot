package com.project.userservice.controller;

import com.project.userservice.payload.response.UserCoinbaseTokenResponse;
import com.project.userservice.service.CoinbaseTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserCoinbaseController {

    private final CoinbaseTokenService coinbaseTokenService;

    @GetMapping("/{userId}/coinbase/valid-token")
    public ResponseEntity<UserCoinbaseTokenResponse> getValidToken(@PathVariable String userId) {
        UserCoinbaseTokenResponse result = coinbaseTokenService.getValidToken(userId);
        // you can set HTTP status codes based on success/failure
        if (!result.isLinked()) {
            // might return 400 or 404 if you prefer
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(result);
    }
}
