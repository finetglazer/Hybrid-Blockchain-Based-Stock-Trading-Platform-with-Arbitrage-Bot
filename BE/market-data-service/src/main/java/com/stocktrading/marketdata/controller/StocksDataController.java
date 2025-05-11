package com.stocktrading.marketdata.controller;

import com.stocktrading.marketdata.service.PortfolioService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@AllArgsConstructor
@RequestMapping("market-data/api/v1")
public class StocksDataController {
    private final PortfolioService portfolioService;

    @GetMapping("/me/{accountId}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable String accountId, Principal principal) {
        return ResponseEntity.ok(portfolioService.getPortfolio(accountId, principal.getName()));
    }

    @GetMapping("/me/general-portfolio")
    public ResponseEntity<?> getGeneralPortfolio(Principal principal) {
        return ResponseEntity.ok(portfolioService.getGeneralPortfolio(principal.getName()));
    }
}
