package com.stocktrading.brokerage.model;

import com.stocktrading.brokerage.service.MarketPriceCache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Mock implementation of a stock exchange order book
 * Uses MarketPriceCache for price data
 */
@Slf4j
@Component
@Data
public class MockOrderBook {

    @Autowired
    private MarketPriceCache marketPriceCache;

    // Pending limit orders - key is symbol, value is list of pending orders for that symbol
    private final Map<String, List<PendingOrder>> pendingOrders = new ConcurrentHashMap<>();

    // Random generator for fallback price simulation if cache is empty
    private final Random random = new Random();

    // Default stock prices for initialization - used only if market data is not available
    private final Map<String, BigDecimal> defaultPrices = new ConcurrentHashMap<>();

    /**
     * Initialize with some default stock prices (used only as fallback)
     */
    public MockOrderBook() {
        // Initialize some fallback stock prices
        defaultPrices.put("AAPL", new BigDecimal("185.50"));
        defaultPrices.put("MSFT", new BigDecimal("328.75"));
        defaultPrices.put("GOOGL", new BigDecimal("142.30"));
        defaultPrices.put("AMZN", new BigDecimal("178.25"));
        defaultPrices.put("TSLA", new BigDecimal("245.65"));
    }

    /**
     * Get current price for a stock symbol
     * Prioritizes market data from cache
     */
    public BigDecimal getCurrentPrice(String symbol) {
        // Try to get price from market data cache first
        BigDecimal price = marketPriceCache.getPrice(symbol);

        // If not in cache, use default price or generate a reasonable random price
        if (price == null) {
            log.warn("No market price found for {}. Using fallback price.", symbol);

            if (defaultPrices.containsKey(symbol)) {
                price = defaultPrices.get(symbol);
            } else {
                // Generate a random price between $50 and $500
                price = BigDecimal.valueOf(50 + random.nextInt(450) + random.nextDouble())
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                defaultPrices.put(symbol, price);
            }
        }

        return price;
    }

    /**
     * Get bid price (highest buy order) for a stock
     * Prioritizes market data from cache
     */
    public BigDecimal getBidPrice(String symbol) {
        // Try to get bid price from market data cache first
        BigDecimal bidPrice = marketPriceCache.getBidPrice(symbol);

        // If not in cache, calculate a reasonable bid price from current price
        if (bidPrice == null) {
            BigDecimal currentPrice = getCurrentPrice(symbol);
            double spreadPercent = 0.001 + random.nextDouble() * 0.004; // 0.1% to 0.5% spread
            bidPrice = currentPrice.multiply(BigDecimal.valueOf(1 - spreadPercent))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        return bidPrice;
    }

    /**
     * Get ask price (lowest sell order) for a stock
     * Prioritizes market data from cache
     */
    public BigDecimal getAskPrice(String symbol) {
        // Try to get ask price from market data cache first
        BigDecimal askPrice = marketPriceCache.getAskPrice(symbol);

        // If not in cache, calculate a reasonable ask price from current price
        if (askPrice == null) {
            BigDecimal currentPrice = getCurrentPrice(symbol);
            double spreadPercent = 0.001 + random.nextDouble() * 0.004; // 0.1% to 0.5% spread
            askPrice = currentPrice.multiply(BigDecimal.valueOf(1 + spreadPercent))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        return askPrice;
    }

    /**
     * Add a pending limit order to the order book
     */
    public PendingOrder addPendingOrder(String orderId, String stockSymbol, String orderType,
                                        String side, Integer quantity, BigDecimal limitPrice,
                                        String timeInForce, String sagaId) {
        // Create pending order
        PendingOrder order = new PendingOrder(
                orderId,
                stockSymbol,
                orderType,
                side,
                quantity,
                limitPrice,
                timeInForce,
                sagaId,
                Instant.now(),
                calculateExpirationTime(timeInForce)
        );

        // Add to pending orders
        pendingOrders.computeIfAbsent(stockSymbol, k -> new CopyOnWriteArrayList<>()).add(order);
        log.info("Added limit order to book: {}", order);

        // Return the pending order object
        return order;
    }

    /**
     * Check if a limit order can be executed at current market prices
     */
    public boolean canExecuteImmediately(String stockSymbol, String side, BigDecimal limitPrice) {
        if ("BUY".equals(side)) {
            // For buy orders, limit price must be >= ask price
            BigDecimal askPrice = getAskPrice(stockSymbol);
            return limitPrice.compareTo(askPrice) >= 0;
        } else if ("SELL".equals(side)) {
            // For sell orders, limit price must be <= bid price
            BigDecimal bidPrice = getBidPrice(stockSymbol);
            return limitPrice.compareTo(bidPrice) <= 0;
        }

        return false;
    }

    /**
     * Get execution price for an order
     */
    public BigDecimal getExecutionPrice(String stockSymbol, String side) {
        if ("BUY".equals(side)) {
            return getAskPrice(stockSymbol);
        } else {
            return getBidPrice(stockSymbol);
        }
    }

    /**
     * Check for orders that can be executed based on current prices
     * @return List of orders that should be executed
     */
    public List<PendingOrder> findExecutableOrders() {
        List<PendingOrder> executableOrders = new ArrayList<>();

        for (Map.Entry<String, List<PendingOrder>> entry : pendingOrders.entrySet()) {
            String symbol = entry.getKey();
            List<PendingOrder> orders = entry.getValue();

            List<PendingOrder> toExecute = orders.stream()
                    .filter(order -> !order.isExpired() && canExecute(order, symbol))
                    .collect(Collectors.toList());

            executableOrders.addAll(toExecute);
        }

        return executableOrders;
    }

    /**
     * Remove a pending order from the order book
     */
    public boolean removePendingOrder(String orderId) {
        for (List<PendingOrder> orders : pendingOrders.values()) {
            Iterator<PendingOrder> iterator = orders.iterator();
            while (iterator.hasNext()) {
                PendingOrder order = iterator.next();
                if (order.getOrderId().equals(orderId)) {
                    orders.remove(order);
                    log.info("Removed pending order: {}", orderId);
                    return true;
                }
            }
        }

        log.warn("Order not found in pending orders: {}", orderId);
        return false;
    }

    /**
     * Find a pending order by ID
     */
    public Optional<PendingOrder> findPendingOrder(String orderId) {
        for (List<PendingOrder> orders : pendingOrders.values()) {
            for (PendingOrder order : orders) {
                if (order.getOrderId().equals(orderId)) {
                    return Optional.of(order);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Check if an order can be executed at current market price
     */
    private boolean canExecute(PendingOrder order, String symbol) {
        if ("BUY".equals(order.getSide())) {
            // Can execute buy if limit price >= ask price
            BigDecimal askPrice = getAskPrice(symbol);
            return order.getLimitPrice().compareTo(askPrice) >= 0;
        } else if ("SELL".equals(order.getSide())) {
            // Can execute sell if limit price <= bid price
            BigDecimal bidPrice = getBidPrice(symbol);
            return order.getLimitPrice().compareTo(bidPrice) <= 0;
        }

        return false;
    }

    /**
     * Find expired orders that need cancellation
     */
    public List<PendingOrder> findExpiredOrders() {
        List<PendingOrder> expiredOrders = new ArrayList<>();
        Instant now = Instant.now();

        for (List<PendingOrder> orders : pendingOrders.values()) {
            for (PendingOrder order : orders) {
                if (order.getExpirationTime() != null && order.getExpirationTime().isBefore(now)) {
                    expiredOrders.add(order);
                }
            }
        }

        return expiredOrders;
    }

    /**
     * Calculate expiration time based on time-in-force
     */
    private Instant calculateExpirationTime(String timeInForce) {
        if ("GTC".equalsIgnoreCase(timeInForce)) {
            // Good Till Cancel - doesn't expire automatically
            return null;
        } else if ("DAY".equalsIgnoreCase(timeInForce)) {
            // End of trading day (assuming 4:00 PM Eastern Time)
            LocalDate today = LocalDate.now();
            LocalTime marketClose = LocalTime.of(16, 0); // 4:00 PM
            return today.atTime(marketClose).atZone(ZoneId.of("America/New_York")).toInstant();
        } else if (timeInForce != null && timeInForce.startsWith("GTD-")) {
            // Good Till Date - format GTD-YYYY-MM-DD
            try {
                String dateStr = timeInForce.substring(4);
                LocalDate date = LocalDate.parse(dateStr);
                LocalTime marketClose = LocalTime.of(16, 0); // 4:00 PM
                return date.atTime(marketClose).atZone(ZoneId.of("America/New_York")).toInstant();
            } catch (Exception e) {
                log.error("Invalid GTD date format: {}", timeInForce);
                // Default to DAY order
                LocalDate today = LocalDate.now();
                LocalTime marketClose = LocalTime.of(16, 0);
                return today.atTime(marketClose).atZone(ZoneId.of("America/New_York")).toInstant();
            }
        } else {
            // Default to DAY order
            LocalDate today = LocalDate.now();
            LocalTime marketClose = LocalTime.of(16, 0);
            return today.atTime(marketClose).atZone(ZoneId.of("America/New_York")).toInstant();
        }
    }
}