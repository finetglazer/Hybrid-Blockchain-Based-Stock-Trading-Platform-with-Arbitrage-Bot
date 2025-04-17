package com.stocktrading.brokerage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A simple mock implementation of a stock order book
 */
@Slf4j
@Component
public class MockOrderBook {

    // Map to store current market prices for each symbol
    private final Map<String, StockPrice> marketPrices;

    public MockOrderBook() {
        this.marketPrices = new ConcurrentHashMap<>();

        // Initialize with some default stocks
        initializeDefaultStocks();
    }

    private void initializeDefaultStocks() {
        // Initialize some default stocks with mock prices
        addStock("AAPL", new BigDecimal("180.50"));
        addStock("GOOGL", new BigDecimal("2750.75"));
        addStock("MSFT", new BigDecimal("340.25"));
        addStock("AMZN", new BigDecimal("3300.50"));
        addStock("TSLA", new BigDecimal("850.20"));
        addStock("FB", new BigDecimal("330.15"));
        addStock("NFLX", new BigDecimal("550.80"));
        addStock("JPM", new BigDecimal("150.40"));
        addStock("V", new BigDecimal("225.60"));
        addStock("JNJ", new BigDecimal("165.30"));
    }

    /**
     * Add a stock to the order book
     */
    public void addStock(String symbol, BigDecimal price) {
        StockPrice stockPrice = StockPrice.builder()
                .symbol(symbol)
                .currentPrice(price)
                .bid(price.subtract(randomVariance(price, 0.01)))
                .ask(price.add(randomVariance(price, 0.01)))
                .build();

        marketPrices.put(symbol, stockPrice);
    }

    /**
     * Get the current price for a stock
     */
    public BigDecimal getCurrentPrice(String symbol) {
        StockPrice stockPrice = marketPrices.get(symbol);
        if (stockPrice == null) {
            // If stock doesn't exist, create it with a random price
            BigDecimal randomPrice = new BigDecimal(ThreadLocalRandom.current().nextDouble(50, 500))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            addStock(symbol, randomPrice);
            return randomPrice;
        }

        // Simulate price movement each time it's queried
        simulatePriceMovement(stockPrice);
        return stockPrice.getCurrentPrice();
    }

    /**
     * Get the bid price for a stock
     */
    public BigDecimal getBidPrice(String symbol) {
        StockPrice stockPrice = marketPrices.get(symbol);
        if (stockPrice == null) {
            getCurrentPrice(symbol); // This will create the stock if it doesn't exist
            stockPrice = marketPrices.get(symbol);
        }
        return stockPrice.getBid();
    }

    /**
     * Get the ask price for a stock
     */
    public BigDecimal getAskPrice(String symbol) {
        StockPrice stockPrice = marketPrices.get(symbol);
        if (stockPrice == null) {
            getCurrentPrice(symbol); // This will create the stock if it doesn't exist
            stockPrice = marketPrices.get(symbol);
        }
        return stockPrice.getAsk();
    }

    /**
     * Simulate market price movement
     */
    private void simulatePriceMovement(StockPrice stockPrice) {
        BigDecimal currentPrice = stockPrice.getCurrentPrice();

        // Generate a small random price movement (±1%)
        BigDecimal movement = randomVariance(currentPrice, 0.01);

        // Randomly decide if price goes up or down
        if (ThreadLocalRandom.current().nextBoolean()) {
            currentPrice = currentPrice.add(movement);
        } else {
            currentPrice = currentPrice.subtract(movement);
        }

        // Ensure price doesn't go below $1
        if (currentPrice.compareTo(BigDecimal.ONE) < 0) {
            currentPrice = BigDecimal.ONE;
        }

        // Update the stock price
        stockPrice.setCurrentPrice(currentPrice);
        stockPrice.setBid(currentPrice.subtract(randomVariance(currentPrice, 0.01)));
        stockPrice.setAsk(currentPrice.add(randomVariance(currentPrice, 0.01)));
    }

    /**
     * Generate a random variance based on a percentage of the price
     */
    private BigDecimal randomVariance(BigDecimal price, double percentage) {
        double variance = price.doubleValue() * percentage;
        double randomVariance = ThreadLocalRandom.current().nextDouble(0, variance);
        return new BigDecimal(randomVariance).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Internal class to represent stock price data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class StockPrice {
        private String symbol;
        private BigDecimal currentPrice;
        private BigDecimal bid;
        private BigDecimal ask;
    }
}