package com.stocktrading.brokerage.model;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.brokerage.service.MarketPriceCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates an order book for the mock brokerage
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockOrderBook {

    private final MarketPriceCache marketPriceCache;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    // Map to store pending limit orders (orderId -> PendingOrder)
    private final Map<String, PendingOrder> pendingOrders = new ConcurrentHashMap<>();

    @Value("${kafka.topics.broker-events}")
    private String brokerEventsTopic;

    @Value("${market.simulation.price-variation:0.02}")
    private double priceVariation; // Default 2% variation for simulated prices

    /**
     * Add a pending order to the order book
     */
    public PendingOrder addPendingOrder(String orderId, String stockSymbol, String orderType,
                                        String side, Integer quantity, BigDecimal limitPrice,
                                        String timeInForce, String sagaId) {

        // Calculate expiration time based on timeInForce
        Instant expirationTime = calculateExpirationTime(timeInForce);

        // Create and store the pending order
        PendingOrder pendingOrder = PendingOrder.builder()
                .orderId(orderId)
                .stockSymbol(stockSymbol)
                .orderType(orderType)
                .side(side)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .timeInForce(timeInForce)
                .createdAt(Instant.now())
                .expirationTime(expirationTime)
                .sagaId(sagaId)
                .build();

        pendingOrders.put(orderId, pendingOrder);

        log.info("LIMIT ORDER ADDED: Added pending {} order to order book: {} at limit price {}. Total pending orders: {}",
                side, orderId, limitPrice, pendingOrders.size());

        return pendingOrder;
    }

    /**
     * Remove a pending order from the order book
     */
    public boolean removePendingOrder(String orderId) {
        PendingOrder removed = pendingOrders.remove(orderId);
        if (removed != null) {
            log.info("Removed pending order from order book: {}", orderId);
            return true;
        }
        return false;
    }

    /**
     * Find a pending order by orderId
     */
    public Optional<PendingOrder> findPendingOrder(String orderId) {
        return Optional.ofNullable(pendingOrders.get(orderId));
    }

    /**
     * Get the current number of pending orders
     */
    public int getPendingOrderCount() {
        return pendingOrders.size();
    }

    /**
     * Get the current market price for a stock
     */
    public BigDecimal getCurrentPrice(String stockSymbol) {
        // Try to get from price cache first
        BigDecimal cachedPrice = marketPriceCache.getPrice(stockSymbol);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        // Fallback to generating a random price if not in cache
        return generateRandomPrice(stockSymbol);
    }

    /**
     * Check if a limit order can be executed immediately based on current market prices
     */
    public boolean canExecuteImmediately(String stockSymbol, String side, BigDecimal limitPrice) {
        if ("BUY".equals(side)) {
            // For buy orders, can execute if askPrice <= limitPrice
            BigDecimal askPrice = getAskPrice(stockSymbol);
            return askPrice.compareTo(limitPrice) <= 0;
        } else if ("SELL".equals(side)) {
            // For sell orders, can execute if bidPrice >= limitPrice
            BigDecimal bidPrice = getBidPrice(stockSymbol);
            return bidPrice.compareTo(limitPrice) >= 0;
        }

        return false;
    }

    /**
     * Get execution price for an order - uses ask price for buy orders, bid price for sell orders
     */
    public BigDecimal getExecutionPrice(String stockSymbol, String side) {
        if ("BUY".equals(side)) {
            return getAskPrice(stockSymbol);
        } else {
            return getBidPrice(stockSymbol);
        }
    }

    /**
     * Get the ask price (what buyers pay) for a stock
     */
    public BigDecimal getAskPrice(String stockSymbol) {
        // Try to get from price cache first
        BigDecimal cachedAskPrice = marketPriceCache.getAskPrice(stockSymbol);
        if (cachedAskPrice != null) {
            return cachedAskPrice;
        }

        // Fallback: Calculate from current price with a small spread
        BigDecimal currentPrice = getCurrentPrice(stockSymbol);
        return currentPrice.multiply(BigDecimal.valueOf(1.001)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get the bid price (what sellers receive) for a stock
     */
    public BigDecimal getBidPrice(String stockSymbol) {
        // Try to get from price cache first
        BigDecimal cachedBidPrice = marketPriceCache.getBidPrice(stockSymbol);
        if (cachedBidPrice != null) {
            return cachedBidPrice;
        }

        // Fallback: Calculate from current price with a small spread
        BigDecimal currentPrice = getCurrentPrice(stockSymbol);
        return currentPrice.multiply(BigDecimal.valueOf(0.999)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate a random price for a stock symbol
     */
    private BigDecimal generateRandomPrice(String stockSymbol) {
        // Deterministic but unique base price for each symbol based on hash code
        int hash = Math.abs(stockSymbol.hashCode()) % 1000;
        double basePrice = 50 + hash % 450; // Base price between $50 and $500

        // Add some randomness to make it slightly different each time
        basePrice = basePrice * (1 + (random.nextDouble() * priceVariation * 2 - priceVariation));

        return BigDecimal.valueOf(basePrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate expiration time based on timeInForce
     */
    private Instant calculateExpirationTime(String timeInForce) {
        if (timeInForce == null) {
            timeInForce = "DAY"; // Default
        }

        switch (timeInForce) {
            case "DAY":
                // Expires at end of trading day (for simulation, 8 hours from now)
                return Instant.now().plusSeconds(8 * 60 * 60);
            case "GTC": // Good Till Canceled
                // Doesn't expire automatically (use 30 days for simulation)
                return Instant.now().plusSeconds(30 * 24 * 60 * 60);
            case "IOC": // Immediate or Cancel
                // Expires immediately if not fully filled
                return Instant.now();
            case "FOK": // Fill or Kill
                // Must be executed immediately and completely or not at all
                return Instant.now();
            case "GTD": // Good Till Date (using default of 3 days for simulation)
                return Instant.now().plusSeconds(3 * 24 * 60 * 60);
            default:
                return Instant.now().plusSeconds(8 * 60 * 60); // Default to DAY
        }
    }

    /**
     * Scheduled task to check for expired orders
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000)
    public void checkForExpiredOrders() {
        // Skip if no pending orders to check
        if (pendingOrders.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        log.debug("Checking for expired orders at {}", now);

        List<String> expiredOrderIds = new ArrayList<>();

        // Find expired orders
        for (PendingOrder order : pendingOrders.values()) {
            if (order.getExpirationTime() != null && order.getExpirationTime().isBefore(now)) {
                expiredOrderIds.add(order.getOrderId());

                // Send ORDER_EXPIRED event
                sendOrderExpiredEvent(order);
            }
        }

        // Remove expired orders
        for (String orderId : expiredOrderIds) {
            pendingOrders.remove(orderId);
            log.info("Removed expired order: {}", orderId);
        }

        if (!expiredOrderIds.isEmpty()) {
            log.info("Removed {} expired orders", expiredOrderIds.size());
        }
    }

    /**
     * Send an ORDER_EXPIRED event for a limit order
     */
    private void sendOrderExpiredEvent(PendingOrder order) {
        try {
            EventMessage event = EventMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sagaId(order.getSagaId())
                    .type("ORDER_EXPIRED")
                    .sourceService("MOCK_BROKERAGE_SERVICE")
                    .timestamp(Instant.now())
                    .success(true)
                    .build();

            event.setPayloadValue("orderId", order.getOrderId());
            event.setPayloadValue("stockSymbol", order.getStockSymbol());
            event.setPayloadValue("limitPrice", order.getLimitPrice());
            event.setPayloadValue("expiredAt", Instant.now().toString());
            event.setPayloadValue("status", "EXPIRED");

            kafkaTemplate.send(brokerEventsTopic, order.getSagaId(), event);
            log.info("Sent ORDER_EXPIRED event for order: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Error sending ORDER_EXPIRED event", e);
        }
    }

    /**
     * Scheduled task to check pending limit orders against current market prices
     * Runs every 2 seconds, but only if there are pending orders to check
     */
    @Scheduled(fixedRate = 5000)
    public void checkPendingLimitOrders() {
        if (pendingOrders.isEmpty()) {
            return; // Nothing to check
        }

        // Only log when actually checking orders (not every 2 seconds)
        log.debug("Checking pending limit orders. Count: {}", pendingOrders.size());

        // Create a copy to avoid concurrent modification
        List<PendingOrder> ordersToCheck = new ArrayList<>(pendingOrders.values());

        for (PendingOrder order : ordersToCheck) {
            // For now, only BUY orders are supported
            if (!"BUY".equals(order.getSide())) {
                continue;
            }

            checkAndExecuteBuyOrder(order);
        }
    }

    /**
     * Check and execute a buy order if price conditions are met
     */
    private void checkAndExecuteBuyOrder(PendingOrder order) {
        String stockSymbol = order.getStockSymbol();
        BigDecimal limitPrice = order.getLimitPrice();

        // Get current ask price
        BigDecimal currentAskPrice = getAskPrice(stockSymbol);

        // For BUY orders, execute if askPrice <= limitPrice
        boolean shouldExecute = currentAskPrice.compareTo(limitPrice) <= 0;

        // Changed to trace level to reduce output
        log.trace("Checking BUY order {} - limitPrice: {}, currentAskPrice: {}, shouldExecute: {}",
                order.getOrderId(), limitPrice, currentAskPrice, shouldExecute);

        if (shouldExecute) {
            executeOrder(order, currentAskPrice);
        }
    }

    /**
     * Execute a pending limit order at the specified price
     */
    private void executeOrder(PendingOrder order, BigDecimal executionPrice) {
        // First remove from pending orders
        pendingOrders.remove(order.getOrderId());

        log.info("Executing limit order: {} for {} shares of {} at price {}",
                order.getOrderId(), order.getQuantity(), order.getStockSymbol(), executionPrice);

        // Generate a broker order ID
        String brokerOrderId = "MBS-LMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try {
            // Send ORDER_EXECUTED_BY_BROKER event
            EventMessage event = EventMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sagaId(order.getSagaId())
                    .type("ORDER_EXECUTED_BY_BROKER")
                    .sourceService("MOCK_BROKERAGE_SERVICE")
                    .timestamp(Instant.now())
                    .success(true)
                    .build();

            event.setPayloadValue("orderId", order.getOrderId());
            event.setPayloadValue("brokerOrderId", brokerOrderId);
            event.setPayloadValue("stockSymbol", order.getStockSymbol());
            event.setPayloadValue("executionPrice", executionPrice);
            event.setPayloadValue("executedQuantity", order.getQuantity());
            event.setPayloadValue("executedAt", Instant.now().toString());
            event.setPayloadValue("status", "FILLED");

            kafkaTemplate.send(brokerEventsTopic, order.getSagaId(), event);
            log.info("Sent ORDER_EXECUTED_BY_BROKER event for limit order: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Error sending ORDER_EXECUTED_BY_BROKER event", e);
        }
    }
}