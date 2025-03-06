package com.project.userservice.controller;

import com.project.userservice.common.BaseResponse;
import com.project.userservice.payload.request.internal.SaveCoinbaseTokensRequest;
import com.project.userservice.payload.response.internal.UserCoinbaseTokenResponse;
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
        // might return 400 or 404 if you prefer
        return ResponseEntity.ok(result);
    }

    @PostMapping("/coinbase/save-tokens")
    public BaseResponse<?> saveCoinbaseTokens(@RequestBody SaveCoinbaseTokensRequest request) {
        return coinbaseTokenService.saveCoinbaseTokens(request);
    }
}
