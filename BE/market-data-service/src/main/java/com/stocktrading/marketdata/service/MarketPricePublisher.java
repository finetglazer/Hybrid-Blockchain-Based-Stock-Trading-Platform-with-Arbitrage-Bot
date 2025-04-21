package com.stocktrading.marketdata.service;

import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that periodically publishes market price updates
 * Uses random price generation for demonstration
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class MarketPricePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    // Map to store the last price for each symbol to create realistic price movements
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    // Default symbols to track
    private final List<String> trackedSymbols = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM", "V", "JNJ"
    );

    @Value("${kafka.topics.market-price-updates:market.price.updates}")
    private String marketPriceUpdatesTopic;

    /**
     * Initialize default prices on startup
     */
    public void init() {
        // Initialize with reasonable starting prices
        lastPrices.put("AAPL", new BigDecimal("185.50"));
        lastPrices.put("MSFT", new BigDecimal("328.75"));
        lastPrices.put("GOOGL", new BigDecimal("142.30"));
        lastPrices.put("AMZN", new BigDecimal("178.25"));
        lastPrices.put("TSLA", new BigDecimal("245.65"));
        lastPrices.put("META", new BigDecimal("326.90"));
        lastPrices.put("NVDA", new BigDecimal("450.20"));
        lastPrices.put("JPM", new BigDecimal("153.40"));
        lastPrices.put("V", new BigDecimal("275.60"));
        lastPrices.put("JNJ", new BigDecimal("156.80"));
    }

    /**
     * Publish price updates for all tracked stocks every 5 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void publishPriceUpdates() {
        log.debug("Publishing market price updates");

        try {
            // Ensure we have initialized prices
            if (lastPrices.isEmpty()) {
                init();
            }

            // Generate and publish updates for each tracked symbol
            for (String symbol : trackedSymbols) {
                // Get or create last price
                BigDecimal lastPrice = lastPrices.getOrDefault(symbol,
                        BigDecimal.valueOf(100 + random.nextInt(400)));

                // Calculate a random price change (-2% to +2%)
                double changePercent = (random.nextDouble() * 4 - 2) / 100.0;
                BigDecimal newPrice = lastPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);

                // Calculate bid/ask with a random spread (0.1% to 0.3%)
                double spreadPercent = (0.1 + random.nextDouble() * 0.2) / 100.0;
                BigDecimal halfSpread = newPrice.multiply(BigDecimal.valueOf(spreadPercent / 2))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);

                BigDecimal bidPrice = newPrice.subtract(halfSpread);
                BigDecimal askPrice = newPrice.add(halfSpread);

                // Generate a random volume between 1,000 and 100,000
                long volume = 1000 + random.nextInt(99000);

                // Create and send event
                EventMessage event = EventMessage.builder()
                        .messageId(UUID.randomUUID().toString())
                        .type("MARKET_PRICES_UPDATED")
                        .sourceService("MARKET_DATA_SERVICE")
                        .timestamp(Instant.now())
                        .success(true)
                        .build();

                // Set payload with price data
                event.setPayloadValue("symbol", symbol);
                event.setPayloadValue("price", newPrice);
                event.setPayloadValue("bidPrice", bidPrice);
                event.setPayloadValue("askPrice", askPrice);
                event.setPayloadValue("volume", volume);
                event.setPayloadValue("timestamp", Instant.now().toString());
                event.setPayloadValue("change", newPrice.subtract(lastPrice));
                event.setPayloadValue("changePercent", BigDecimal.valueOf(changePercent * 100).setScale(2, BigDecimal.ROUND_HALF_UP));

                // Store the new price for next update
                lastPrices.put(symbol, newPrice);

                // Publish with symbol as key for partitioning
                kafkaTemplate.send(marketPriceUpdatesTopic, symbol, event);

                log.debug("Published price update for {}: {} (bid: {}, ask: {})",
                        symbol, newPrice, bidPrice, askPrice);
            }

            log.debug("Published price updates for {} stocks", trackedSymbols.size());

        } catch (Exception e) {
            log.error("Error publishing market price updates", e);
        }
    }

    /**
     * Add a symbol to track
     */
    public void addSymbol(String symbol) {
        if (!trackedSymbols.contains(symbol)) {
            trackedSymbols.add(symbol);
            // Initialize with a reasonable price if not already present
            if (!lastPrices.containsKey(symbol)) {
                lastPrices.put(symbol, BigDecimal.valueOf(100 + random.nextInt(400)));
            }
            log.info("Added symbol {} to tracked list", symbol);
        }
    }
}