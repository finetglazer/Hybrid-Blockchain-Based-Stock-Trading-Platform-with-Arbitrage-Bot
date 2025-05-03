package com.stocktrading.marketdata.websocket;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.marketdata.service.MarketPricePublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MarketDataWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketPricePublisher marketPricePublisher;

    // Store sessions and their filter preferences
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final Map<String, Map<String, Object>> stockData = new ConcurrentHashMap<>();

    // Historical data for sparklines - keep last 50 data points for each stock
    private final Map<String, List<Map<String, Object>>> stockHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_POINTS = 50;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("New WebSocket connection established: {}", session.getId());
        sessions.add(session);

        // Send initial stock data to new connection
        try {
            log.info("Sending initial data to session: {}", session.getId());
            sendInitialData(session);
            log.info("Initial data sent successfully to session: {}", session.getId());
        } catch (IOException e) {
            log.error("Error sending initial data to client", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // Handle client messages (filtering, etc.)
            Map<String, Object> request = objectMapper.readValue(message.getPayload(), Map.class);

            if (request.containsKey("filter")) {
                String filter = (String) request.get("filter");
                // Implement filtering logic
                sendFilteredData(session, filter);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.market-price-updates}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMarketPriceUpdate(EventMessage event) {
        if ("MARKET_PRICES_UPDATED".equals(event.getType())) {
            try {
                String symbol = event.getPayloadValue("symbol");
                BigDecimal price = BigDecimal.valueOf((Double) event.getPayloadValue("price"));
                BigDecimal bidPrice = BigDecimal.valueOf((Double) event.getPayloadValue("bidPrice"));
                BigDecimal askPrice = BigDecimal.valueOf((Double) event.getPayloadValue("askPrice"));
                Object volumeObj = event.getPayloadValue("volume");
                Long volume;
                if (volumeObj instanceof Integer) {
                    volume = ((Integer) volumeObj).longValue();
                } else if (volumeObj instanceof Long) {
                    volume = (Long) volumeObj;
                } else if (volumeObj instanceof Number) {
                    volume = ((Number) volumeObj).longValue();
                } else {
                    volume = 0L; // Default value
                }

                // Update current stock data
                Map<String, Object> stockInfo = new HashMap<>();
                stockInfo.put("symbol", symbol);
                stockInfo.put("name", getCompanyName(symbol));
                stockInfo.put("price", price);
                stockInfo.put("bidPrice", bidPrice);
                stockInfo.put("askPrice", askPrice);
                stockInfo.put("volume", volume);
                stockInfo.put("timestamp", Instant.now().toString());

                // Calculate percent change (for now using a simple simulation)
                BigDecimal previousPrice = stockData.containsKey(symbol) ?
                        (BigDecimal) stockData.get(symbol).get("price") : price;
                if (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal changePercent = price.subtract(previousPrice)
                            .divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal(100));
                    stockInfo.put("changePercent", changePercent);
                }

                stockData.put(symbol, stockInfo);

                // Update historical data for sparklines
                updateHistoricalData(symbol, stockInfo);

                // Broadcast to all connected clients
                broadcastUpdate(symbol, stockInfo);

            } catch (Exception e) {
                log.error("Error processing market price update", e);
            }
        }
    }

    private void updateHistoricalData(String symbol, Map<String, Object> stockInfo) {
        if (!stockHistory.containsKey(symbol)) {
            stockHistory.put(symbol, new ArrayList<>());
        }

        List<Map<String, Object>> history = stockHistory.get(symbol);

        // Create a copy of relevant data for history
        Map<String, Object> historyPoint = new HashMap<>();
        historyPoint.put("price", stockInfo.get("price"));
        historyPoint.put("volume", stockInfo.get("volume"));
        historyPoint.put("timestamp", stockInfo.get("timestamp"));

        history.add(historyPoint);

        // Trim history to maximum size
        if (history.size() > MAX_HISTORY_POINTS) {
            history = history.subList(history.size() - MAX_HISTORY_POINTS, history.size());
            stockHistory.put(symbol, history);
        }
    }

    private void sendInitialData(WebSocketSession session) throws IOException {
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("type", "initialData");
        initialData.put("stocks", new ArrayList<>(stockData.values()));

        // Limit history data size - only send most recent 20 points per stock
        Map<String, List<Map<String, Object>>> limitedHistory = new HashMap<>();
        stockHistory.forEach((symbol, history) -> {
            int size = history.size();
            int start = Math.max(0, size - 20); // Only take last 20 points
            limitedHistory.put(symbol, history.subList(start, size));
        });

        initialData.put("history", limitedHistory);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initialData)));
    }

    private void sendFilteredData(WebSocketSession session, String filter) throws IOException {
        List<Map<String, Object>> filteredStocks = new ArrayList<>();

        for (Map<String, Object> stock : stockData.values()) {
            String symbol = (String) stock.get("symbol");
            String name = (String) stock.get("name");

            if (filter == null || filter.isEmpty() ||
                    symbol.toLowerCase().contains(filter.toLowerCase()) ||
                    name.toLowerCase().contains(filter.toLowerCase())) {
                filteredStocks.add(stock);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "filteredData");
        response.put("stocks", filteredStocks);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void broadcastUpdate(String symbol, Map<String, Object> stockInfo) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "update");
        update.put("symbol", symbol);
        update.put("data", stockInfo);

        // Add historical data for sparklines
        if (stockHistory.containsKey(symbol)) {
            update.put("history", stockHistory.get(symbol));
        }

        String message;
        try {
            message = objectMapper.writeValueAsString(update);

            List<WebSocketSession> sessionsToRemove = new ArrayList<>();

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        synchronized (session) {
                            // Synchronize on the session to prevent concurrent sends
                            session.sendMessage(new TextMessage(message));
                        }
                    } else {
                        // Session is closed, mark for removal
                        sessionsToRemove.add(session);
                    }
                } catch (IOException e) {
                    log.warn("Error sending message to session {}: {}", session.getId(), e.getMessage());
                    // If there was an error, mark the session for removal
                    sessionsToRemove.add(session);
                }
            }

            // Remove any problematic sessions after iteration
            if (!sessionsToRemove.isEmpty()) {
                sessions.removeAll(sessionsToRemove);
                log.info("Removed {} problematic WebSocket sessions", sessionsToRemove.size());
            }
        } catch (IOException e) {
            log.error("Error preparing broadcast message", e);
        }
    }

    // Mock method for company names - in production, you'd load this from a database
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