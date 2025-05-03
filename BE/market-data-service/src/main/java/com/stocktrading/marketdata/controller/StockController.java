package com.stocktrading.marketdata.controller;

import com.stocktrading.marketdata.service.MarketPricePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // In production, restrict to specific origins
public class StockController {

    private final MarketPricePublisher marketPricePublisher;

    /**
     * Get all stocks data
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllStocks() {
        List<Map<String, Object>> stocks = marketPricePublisher.getAllStockData();

        Map<String, Object> response = new HashMap<>();
        response.put("stocks", stocks);

        return ResponseEntity.ok(response);
    }

    /**
     * Get data for a specific stock
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<Map<String, Object>> getStockData(@PathVariable String symbol) {
        Map<String, Object> stockData = marketPricePublisher.getCurrentStockData(symbol);

        if (stockData == null) {
            return ResponseEntity.notFound().build();
        }

        // Add historical data
        stockData.put("history", marketPricePublisher.getHistoricalData(symbol));

        return ResponseEntity.ok(stockData);
    }

    /**
     * Add a new stock to track
     */
//    @PostMapping
//    public ResponseEntity<Map<String, String>> addStock(@RequestBody Map<String, String> request) {
//        String symbol = request.get("symbol");
//
//        if (symbol == null || symbol.trim().isEmpty()) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Symbol is required"));
//        }
//
//        // Normalize symbol
//        symbol = symbol.trim().toUpperCase();
//
//        marketPricePublisher.addSymbol(symbol);
//
//        return ResponseEntity.ok(Map.of("message", "Stock added successfully", "symbol", symbol));
//    }
}