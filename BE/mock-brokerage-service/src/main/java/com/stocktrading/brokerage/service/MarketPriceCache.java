package com.stocktrading.brokerage.service;

import com.project.kafkamessagemodels.model.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a local cache of market prices that gets updated via Kafka events
 */
@Slf4j
@Component
public class MarketPriceCache {

    // Cache of current market prices
    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();

    // Cache of bid prices
    private final Map<String, BigDecimal> bidPrices = new ConcurrentHashMap<>();

    // Cache of ask prices
    private final Map<String, BigDecimal> askPrices = new ConcurrentHashMap<>();

    /**
     * Get the current market price for a stock symbol
     *
     * @param symbol The stock symbol
     * @return The current price, or null if not in cache
     */
    public BigDecimal getPrice(String symbol) {
        return priceCache.get(symbol);
    }

    /**
     * Get the current bid price for a stock symbol
     *
     * @param symbol The stock symbol
     * @return The current bid price, or null if not in cache
     */
    public BigDecimal getBidPrice(String symbol) {
        return bidPrices.get(symbol);
    }

    /**
     * Get the current ask price for a stock symbol
     *
     * @param symbol The stock symbol
     * @return The current ask price, or null if not in cache
     */
    public BigDecimal getAskPrice(String symbol) {
        return askPrices.get(symbol);
    }

    /**
     * Listen for market price updates from the market data service
     */
    @KafkaListener(
            topics = "${kafka.topics.market-price-updates}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMarketPriceUpdates(EventMessage event) {
        if ("MARKET_PRICES_UPDATED".equals(event.getType())) {
            try {
                // Extract the symbol and price data
                String symbol = event.getPayloadValue("symbol");
                Object priceObj = event.getPayloadValue("price");
                Object bidObj = event.getPayloadValue("bidPrice");
                Object askObj = event.getPayloadValue("askPrice");

                if (symbol != null && priceObj != null) {
                    // Convert price to BigDecimal
                    BigDecimal price = convertToBigDecimal(priceObj);
                    priceCache.put(symbol, price);

                    // Update bid/ask prices if available
                    if (bidObj != null) {
                        bidPrices.put(symbol, convertToBigDecimal(bidObj));
                    }

                    if (askObj != null) {
                        askPrices.put(symbol, convertToBigDecimal(askObj));
                    }

                    // Changed from debug to trace to reduce terminal output
                    log.trace("Updated price cache for {}: price={}, bid={}, ask={}",
                            symbol, price, bidPrices.get(symbol), askPrices.get(symbol));
                }
            } catch (Exception e) {
                log.error("Error processing market price update", e);
            }
        }
    }

    /**
     * Helper method to convert different numeric formats to BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        } else {
            throw new IllegalArgumentException("Cannot convert to BigDecimal: " + value);
        }
    }
}