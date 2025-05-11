package com.stocktrading.marketdata.service;

import com.project.kafkamessagemodels.model.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final Map<String, StockData> stockDataMap = new ConcurrentHashMap<>();

    // Store historical data for each stock (for generating sparklines)
    private final Map<String, List<StockDataPoint>> historicalData = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_POINTS = 50;

    // Default symbols to track
    private final List<String> trackedSymbols = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM", "V", "JNJ",
            "ABBV", "WMT", "PG", "MA", "UNH"
    );

    @Value("${kafka.topics.market-price-updates:market.price.updates}")
    private String marketPriceUpdatesTopic;

    // Data class to hold stock information
    private static class StockData {
        BigDecimal currentPrice;
        BigDecimal previousPrice;
        BigDecimal openPrice;
        BigDecimal highPrice;
        BigDecimal lowPrice;
        BigDecimal bidPrice;
        BigDecimal askPrice;
        long volume;
        long cumulativeVolume;
        Instant lastUpdate;

        // For calculating percent changes
        BigDecimal dayStartPrice;
        BigDecimal weekStartPrice;
        BigDecimal monthStartPrice;

        public BigDecimal getPercentChange() {
            if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            return currentPrice.subtract(previousPrice)
                    .divide(previousPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        public BigDecimal getDayPercentChange() {
            if (dayStartPrice == null || dayStartPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return getPercentChange();
            }

            return currentPrice.subtract(dayStartPrice)
                    .divide(dayStartPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        public BigDecimal getWeekPercentChange() {
            if (weekStartPrice == null || weekStartPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return getDayPercentChange();
            }

            return currentPrice.subtract(weekStartPrice)
                    .divide(weekStartPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        public BigDecimal getMonthPercentChange() {
            if (monthStartPrice == null || monthStartPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return getWeekPercentChange();
            }

            return currentPrice.subtract(monthStartPrice)
                    .divide(monthStartPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }
    }

    // Data point for historical data
    private static class StockDataPoint {
        BigDecimal price;
        BigDecimal volume;
        Instant timestamp;

        public StockDataPoint(BigDecimal price, BigDecimal volume, Instant timestamp) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("price", price);
            map.put("volume", volume);
            map.put("timestamp", timestamp.toString());
            return map;
        }
    }

    /**
     * Initialize default prices on startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing market price publisher with {} stocks", trackedSymbols.size());

        // Initialize with reasonable starting prices
        Map<String, BigDecimal> initialPrices = new HashMap<>();
        initialPrices.put("AAPL", new BigDecimal("185.50"));
        initialPrices.put("MSFT", new BigDecimal("328.75"));
        initialPrices.put("GOOGL", new BigDecimal("142.30"));
        initialPrices.put("AMZN", new BigDecimal("178.25"));
        initialPrices.put("TSLA", new BigDecimal("245.65"));
        initialPrices.put("META", new BigDecimal("326.90"));
        initialPrices.put("NVDA", new BigDecimal("450.20"));
        initialPrices.put("JPM", new BigDecimal("153.40"));
        initialPrices.put("V", new BigDecimal("275.60"));
        initialPrices.put("JNJ", new BigDecimal("156.80"));
        initialPrices.put("ABBV", new BigDecimal("170.25"));
        initialPrices.put("WMT", new BigDecimal("165.40"));
        initialPrices.put("PG", new BigDecimal("145.60"));
        initialPrices.put("MA", new BigDecimal("405.80"));
        initialPrices.put("UNH", new BigDecimal("530.20"));

        Instant now = Instant.now();

        for (String symbol : trackedSymbols) {
            BigDecimal initialPrice = initialPrices.getOrDefault(
                    symbol, BigDecimal.valueOf(100 + random.nextInt(400)));

            StockData data = new StockData();
            data.currentPrice = initialPrice;
            data.previousPrice = initialPrice;
            data.openPrice = initialPrice;
            data.highPrice = initialPrice;
            data.lowPrice = initialPrice;
            data.bidPrice = initialPrice.multiply(BigDecimal.valueOf(0.999)).setScale(2, RoundingMode.HALF_UP);
            data.askPrice = initialPrice.multiply(BigDecimal.valueOf(1.001)).setScale(2, RoundingMode.HALF_UP);
            data.volume = 1000 + random.nextInt(99000);
            data.cumulativeVolume = data.volume;
            data.lastUpdate = now;

            // Initialize reference prices for different time periods
            data.dayStartPrice = initialPrice;
            data.weekStartPrice = initialPrice.multiply(BigDecimal.valueOf(0.9 + random.nextDouble() * 0.2))
                    .setScale(2, RoundingMode.HALF_UP);
            data.monthStartPrice = initialPrice.multiply(BigDecimal.valueOf(0.8 + random.nextDouble() * 0.4))
                    .setScale(2, RoundingMode.HALF_UP);

            stockDataMap.put(symbol, data);
            historicalData.put(symbol, new ArrayList<>());

            // Add initial data point
            addHistoricalDataPoint(symbol, data);
        }
    }

    /**
     * Publish price updates for all tracked stocks every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void publishPriceUpdates() {
        log.info("--- publishPriceUpdates START at {} ---", LocalDateTime.now());
        // Check if market is open (simplified for simulation)
//        LocalDateTime now = LocalDateTime.now();
//        if (now.getDayOfWeek().getValue() >= 6 || // Weekend
//                now.getHour() < 9 || now.getHour() >= 16) { // Outside trading hours (9 AM - 4 PM)
//            // Market closed - no updates
//            return;
//        }

        log.info("Publishing market price updates for {} stocks", trackedSymbols.size());

        try {
            // Generate and publish updates for each tracked symbol
            for (String symbol : trackedSymbols) {
                updateAndPublishStockData(symbol);
            }
        } catch (Exception e) {
            log.error("Error publishing market price updates", e);
        }
    }

    private void updateAndPublishStockData(String symbol) {
        StockData stockData = stockDataMap.getOrDefault(symbol, new StockData());

        // Store previous price
        BigDecimal lastPrice = stockData.currentPrice;

        if (lastPrice == null) {
            lastPrice = BigDecimal.valueOf(100 + random.nextInt(400));
        }

        // Calculate a random price change (-2% to +2%)
        double changePercent = (random.nextDouble() * 4 - 2) / 100.0;
        BigDecimal newPrice = lastPrice.multiply(BigDecimal.valueOf(1 + changePercent))
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate bid/ask with a random spread (0.1% to 0.3%)
        double spreadPercent = (0.1 + random.nextDouble() * 0.2) / 100.0;
        BigDecimal halfSpread = newPrice.multiply(BigDecimal.valueOf(spreadPercent / 2))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal bidPrice = newPrice.subtract(halfSpread);
        BigDecimal askPrice = newPrice.add(halfSpread);

        // Generate a random volume between 1,000 and 100,000
        long volume = 1000 + random.nextInt(99000);

        // Update stock data
        /*
        Explain these below fields:
        - previousPrice: The last price before the update
        - currentPrice: The new price after the update
        - bidPrice: The price at which buyers are willing to buy the stock
        - askPrice: The price at which sellers are willing to sell the stock
        - volume: The number of shares traded in the last update
        - cumulativeVolume: The total volume of shares traded since the last update
        - lastUpdate: The timestamp of the last update
         */
        stockData.previousPrice = lastPrice;
        stockData.currentPrice = newPrice;
        stockData.bidPrice = bidPrice;
        stockData.askPrice = askPrice;
        stockData.volume = volume;
        stockData.cumulativeVolume += volume;
        stockData.lastUpdate = Instant.now();



        // Update high/low prices
        if (stockData.highPrice == null || newPrice.compareTo(stockData.highPrice) > 0) {
            stockData.highPrice = newPrice;
        }
        if (stockData.lowPrice == null || newPrice.compareTo(stockData.lowPrice) < 0) {
            stockData.lowPrice = newPrice;
        }

        // Store in map
        stockDataMap.put(symbol, stockData);

        // Add to historical data
        addHistoricalDataPoint(symbol, stockData);

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
        event.setPayloadValue("changePercent", stockData.getPercentChange());
        event.setPayloadValue("dayChangePercent", stockData.getDayPercentChange());
        event.setPayloadValue("weekChangePercent", stockData.getWeekPercentChange());
        event.setPayloadValue("monthChangePercent", stockData.getMonthPercentChange());

        // Publish with symbol as key for partitioning
        kafkaTemplate.send(marketPriceUpdatesTopic, symbol, event);

        log.info("Published price update for {}: {} (bid: {}, ask: {})",
                symbol, newPrice, bidPrice, askPrice);
    }

    private void addHistoricalDataPoint(String symbol, StockData data) {
        if (!historicalData.containsKey(symbol)) {
            historicalData.put(symbol, new ArrayList<>());
        }

        List<StockDataPoint> history = historicalData.get(symbol);

        // Add new data point
        history.add(new StockDataPoint(
                data.currentPrice,
                BigDecimal.valueOf(data.volume),
                data.lastUpdate
        ));

        // Trim history to maximum size
        if (history.size() > MAX_HISTORY_POINTS) {
            history = history.subList(history.size() - MAX_HISTORY_POINTS, history.size());
            historicalData.put(symbol, history);
        }
    }

    /**
     * Get historical data for a stock as a list of maps
     */
    public List<Map<String, Object>> getHistoricalData(String symbol) {
        List<StockDataPoint> history = historicalData.getOrDefault(symbol, new ArrayList<>());
        List<Map<String, Object>> result = new ArrayList<>();

        for (StockDataPoint point : history) {
            result.add(point.toMap());
        }

        return result;
    }

    /**
     * Get current stock data as a map
     */
    public Map<String, Object> getCurrentStockData(String symbol) {
        StockData data = stockDataMap.get(symbol);
        if (data == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("price", data.currentPrice);
        result.put("bidPrice", data.bidPrice);
        result.put("askPrice", data.askPrice);
        result.put("volume", data.volume);
        result.put("changePercent", data.getPercentChange());
        result.put("dayChangePercent", data.getDayPercentChange());
        result.put("weekChangePercent", data.getWeekPercentChange());
        result.put("monthChangePercent", data.getMonthPercentChange());
        result.put("timestamp", data.lastUpdate.toString());

        return result;
    }

    /**
     * Get all current stock data
     */
    public List<Map<String, Object>> getAllStockData() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (String symbol : trackedSymbols) {
            Map<String, Object> data = getCurrentStockData(symbol);
            if (data != null) {
                // Add company name
                data.put("name", getCompanyName(symbol));
                result.add(data);
            }
        }

        return result;
    }

    /**
     * Add a symbol to track
     */
    public void addSymbol(String symbol) {
        if (!trackedSymbols.contains(symbol)) {
            trackedSymbols.add(symbol);

            // Initialize with a reasonable price if not already present
            if (!stockDataMap.containsKey(symbol)) {
                StockData data = new StockData();
                BigDecimal initialPrice = BigDecimal.valueOf(100 + random.nextInt(400));
                data.currentPrice = initialPrice;
                data.previousPrice = initialPrice;
                data.openPrice = initialPrice;
                data.highPrice = initialPrice;
                data.lowPrice = initialPrice;
                data.bidPrice = initialPrice.multiply(BigDecimal.valueOf(0.999)).setScale(2, RoundingMode.HALF_UP);
                data.askPrice = initialPrice.multiply(BigDecimal.valueOf(1.001)).setScale(2, RoundingMode.HALF_UP);
                data.volume = 1000 + random.nextInt(99000);
                data.cumulativeVolume = data.volume;
                data.lastUpdate = Instant.now();
                data.dayStartPrice = initialPrice;
                data.weekStartPrice = initialPrice.multiply(BigDecimal.valueOf(0.9 + random.nextDouble() * 0.2))
                        .setScale(2, RoundingMode.HALF_UP);
                data.monthStartPrice = initialPrice.multiply(BigDecimal.valueOf(0.8 + random.nextDouble() * 0.4))
                        .setScale(2, RoundingMode.HALF_UP);

                stockDataMap.put(symbol, data);
                historicalData.put(symbol, new ArrayList<>());

                // Add initial data point
                addHistoricalDataPoint(symbol, data);
            }

            log.info("Added symbol {} to tracked list", symbol);
        }
    }

    // Mock method to get company names - in production, you'd load from a database
    private String getCompanyName(String symbol) {
        Map<String, String> companyNames = new HashMap<>();
        companyNames.put("AAPL", "APPLE INC.");
        companyNames.put("MSFT", "MICROSOFT CORP.");
        companyNames.put("GOOGL", "ALPHABET INC.");
        companyNames.put("AMZN", "AMAZON.COM INC.");
        companyNames.put("META", "META PLATFORMS INC.");
        companyNames.put("TSLA", "TESLA INC.");
        companyNames.put("NVDA", "NVIDIA CORP.");
        companyNames.put("JPM", "JPMORGAN CHASE & CO.");
        companyNames.put("V", "VISA INC.");
        companyNames.put("ABBV", "ABBVIE INC.");
        companyNames.put("JNJ", "JOHNSON & JOHNSON");
        companyNames.put("WMT", "WALMART INC.");
        companyNames.put("PG", "PROCTER & GAMBLE CO.");
        companyNames.put("MA", "MASTERCARD INC.");
        companyNames.put("UNH", "UNITEDHEALTH GROUP INC.");

        return companyNames.getOrDefault(symbol, symbol + " CORP.");
    }
}
